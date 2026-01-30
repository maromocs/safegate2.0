package com.SafeGate.service;

import com.SafeGate.entity.PassedPayload;
import com.SafeGate.entity.TestRun;
import com.SafeGate.model.HttpRequestData;
import com.SafeGate.model.SignatureRule;
import com.SafeGate.repository.TestRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service for running dataset tests against the WAF.
 * This service handles the test execution and result tracking.
 */
@Service
public class DatasetTestRunnerService {

    private static final Logger logger = LoggerFactory.getLogger(DatasetTestRunnerService.class);

    @Autowired
    private SignatureRulesEngine rulesEngine;

    @Autowired
    private TestRunRepository testRunRepository;

    @Autowired
    private WafTestModeService testModeService;
    
    @Autowired
    private DatasetParsingService datasetParsingService;
    
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LLMService llmService;

    // Transient storage for last dataset LLM aggregation
    private int lastLlmTotal = 0;
    private int lastLlmMalicious = 0;
    private int lastLlmSafe = 0;
    private final List<Map<String, String>> lastLlmMaliciousList = new ArrayList<>();
    private final List<String> lastPassedPayloadsForLlm = new ArrayList<>();

    public synchronized List<String> consumePassedPayloadsForLlm() {
        List<String> copy = new ArrayList<>(lastPassedPayloadsForLlm);
        lastPassedPayloadsForLlm.clear();
        return copy;
    }

    /**
     * Runs a test using the provided dataset file
     * @param file The dataset file
     * @param datasetFormat The format of the dataset (TXT, CSV, JSON, XML, AUTO)
     * @param attackTypeTag Optional tag for the attack type
     * @param samplingSize Number of attacks to test (All, Random 100, Random 1000, etc.)
     * @param seed Optional seed for deterministic sampling
     * @return The completed test run
     */
    public TestRun runDatasetTest(MultipartFile file, String datasetFormat, String attackTypeTag, String samplingSize, Long seed) {
        if (testModeService.isTestModeEnabled()) {
            throw new IllegalStateException("A test is already in progress.");
        }

        logger.info("Starting dataset test with file: {}, format: {}, attackType: {}, samplingSize: {}, seed: {}", 
                file.getOriginalFilename(), datasetFormat, attackTypeTag, samplingSize, seed);

        // Validate file
        if (file.isEmpty()) {
            logger.error("Uploaded file is empty");
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Start test mode
        testModeService.startTest();
        // Reset transient LLM buffers
        synchronized (lastPassedPayloadsForLlm) {
            lastPassedPayloadsForLlm.clear();
        }
        lastLlmTotal = 0;
        lastLlmMalicious = 0;
        lastLlmSafe = 0;
        lastLlmMaliciousList.clear();

        try {
            // Get the actual current test run to update its metadata
            TestRun testRun = testModeService.getCurrentTestRun();
            if (testRun == null) {
                throw new IllegalStateException("Test run not found after starting");
            }
            
            // Set dataset information
            testRun.setDatasetFileName(file.getOriginalFilename());
            testRun.setDatasetFormat(datasetFormat);
            testRun.setAttackTypeTag(attackTypeTag);
            testRun.setSamplingSize(samplingSize);
            testRun.setSeedNumber(seed);

            logger.info("Parsing dataset file: {}", file.getOriginalFilename());
            
            // Parse the dataset and get HTTP request data using the DatasetParsingService
            List<HttpRequestData> requestDataList = datasetParsingService.getPayloadsFromFile(file, datasetFormat);
            logger.info("Successfully parsed {} HTTP requests from dataset", requestDataList.size());
            
            if (requestDataList.isEmpty()) {
                logger.warn("No HTTP requests were extracted from the dataset file. Please check the file format and content.");
            } else {
                // Log a few sample requests for debugging
                int sampleSize = Math.min(3, requestDataList.size());
                for (int i = 0; i < sampleSize; i++) {
                    HttpRequestData requestData = requestDataList.get(i);
                    String payload = requestData.getPayload();
                    // Truncate long payloads for logging
                    if (payload.length() > 100) {
                        payload = payload.substring(0, 97) + "...";
                    }
                    logger.debug("Sample request {}: {} with payload: {}", i+1, requestData.getMethod(), payload);
                }
            }
            
            // Sample the requests if needed
            logger.info("[DEBUG_LOG] Before sampling: {} requests in dataset", requestDataList.size());
            List<HttpRequestData> sampledRequests = sampleRequests(requestDataList, samplingSize, seed);
            logger.info("After sampling: {} requests selected for testing", sampledRequests.size());
            logger.info("[DEBUG_LOG] Sampled requests size after sampleRequests call: {}", sampledRequests.size());
            
            // Set the total malicious requests
            testRun.setTotalMaliciousRequests(sampledRequests.size());
            logger.info("[DEBUG_LOG] Set totalMaliciousRequests to: {}", sampledRequests.size());
            
            // Process each request by sending HTTP requests
            logger.info("Processing requests by sending them as simulated HTTP requests");
            logger.info("[DEBUG_LOG] About to process {} requests", sampledRequests.size());
            processRequests(sampledRequests, testRun);
            
            // Stop the test and save the results
            logger.info("Test completed. Blocked: {}, Passed: {}", 
                    testRun.getTotalMaliciousBlocked(), 
                    testRun.getTotalMaliciousRequests() - testRun.getTotalMaliciousBlocked());
            
            return testModeService.stopTest();
        } catch (Exception e) {
            // If an error occurs, stop the test and rethrow the exception
            if (testModeService.isTestModeEnabled()) {
                testModeService.stopTest();
            }
            logger.error("Error running dataset test: {}", e.getMessage(), e);
            throw new RuntimeException("Error running dataset test: " + e.getMessage(), e);
        }
    }

    /**
     * Samples the payloads based on the sampling size
     * @param payloads The list of payloads
     * @param samplingSize The sampling size (All, Random 100, Random 1000, etc.)
     * @param seed Optional seed for deterministic sampling
     * @return The sampled list of payloads
     * @deprecated Use sampleRequests instead
     */
    @Deprecated
    private List<String> samplePayloads(List<String> payloads, String samplingSize, Long seed) {
        if (samplingSize == null || samplingSize.equalsIgnoreCase("All")) {
            return payloads;
        }
    
        // Parse the sampling size
        int size = 0;
        if (samplingSize.startsWith("Random ")) {
            String sizeStr = samplingSize.substring(7).replace(",", "");
            try {
                size = Integer.parseInt(sizeStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid sampling size: {}, using all payloads", samplingSize);
                return payloads;
            }
        } else {
            logger.warn("Invalid sampling size format: {}, using all payloads", samplingSize);
            return payloads;
        }
    
        // If the requested sample size is larger than the number of payloads, return all payloads
        if (size >= payloads.size()) {
            return payloads;
        }
    
        // Randomly sample the payloads
        List<String> sampledPayloads = payloads.stream()
                .collect(Collectors.toList()); // Create a copy of the list
        
        if (seed != null) {
            logger.info("Using seed {} for deterministic sampling", seed);
            Collections.shuffle(sampledPayloads, new Random(seed));
        } else {
            Collections.shuffle(sampledPayloads);
        }
        
        // Create a new list instead of a view to avoid potential issues with subList
        return new ArrayList<>(sampledPayloads.subList(0, size));
    }

    /**
     * Samples the HTTP requests based on the sampling size
     * @param requests The list of HTTP requests
     * @param samplingSize The sampling size (All, Random 100, Random 1000, etc.)
     * @param seed Optional seed for deterministic sampling
     * @return The sampled list of HTTP requests
     */
    private List<HttpRequestData> sampleRequests(List<HttpRequestData> requests, String samplingSize, Long seed) {
        logger.info("[DEBUG_LOG] sampleRequests called with samplingSize: {}, seed: {}, original requests size: {}", samplingSize, seed, requests.size());
        
        if (samplingSize == null || samplingSize.equalsIgnoreCase("All")) {
            logger.info("[DEBUG_LOG] Using all requests: {}", requests.size());
            return requests;
        }
    
        // Parse the sampling size
        int size = 0;
        if (samplingSize.startsWith("Random ")) {
            String sizeStr = samplingSize.substring(7).replace(",", "");
            try {
                size = Integer.parseInt(sizeStr);
                logger.info("[DEBUG_LOG] Parsed sampling size: {}", size);
            } catch (NumberFormatException e) {
                logger.warn("Invalid sampling size: {}, using all requests", samplingSize);
                return requests;
            }
        } else {
            logger.warn("Invalid sampling size format: {}, using all requests", samplingSize);
            return requests;
        }
    
        // If the requested sample size is larger than the number of requests, return all requests
        if (size >= requests.size()) {
            logger.info("[DEBUG_LOG] Requested sample size {} is larger than available requests {}, using all requests", size, requests.size());
            return requests;
        }
    
        // Randomly sample the requests
        List<HttpRequestData> sampledRequests = requests.stream()
                .collect(Collectors.toList()); // Create a copy of the list
        
        if (seed != null) {
            logger.info("Using seed {} for deterministic sampling", seed);
            Collections.shuffle(sampledRequests, new Random(seed));
        } else {
            Collections.shuffle(sampledRequests);
        }
        
        // Create a new list instead of a view to avoid potential issues with subList
        List<HttpRequestData> result = new ArrayList<>(sampledRequests.subList(0, size));
        logger.info("[DEBUG_LOG] Final sampled list size: {}", result.size());
        return result;
    }

    /**
     * Processes each payload by sending it as an HTTP request and updates the test run
     * @param payloads The list of payloads to process
     * @param testRun The test run to update
     * @deprecated Use processRequests instead
     */
    @Deprecated
    private void processPayloads(List<String> payloads, TestRun testRun) {
        String testUrl = "http://localhost:8080/api/test/test-harness"; // Correct endpoint that matches our controller
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
    
        logger.info("Processing {} payloads by sending them as simulated HTTP requests...", payloads.size());
    
        // Process each payload
        for (String payload : payloads) {
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            try {
                // Attempt to send the payload
                restTemplate.postForEntity(testUrl, request, String.class);
            
                // If we get here, the WAF did NOT block it (it passed)
                // Removed testModeService.recordPassedRequest() to fix double-counting
                testRun.setTotalPassed(testRun.getTotalPassed() + 1);
                PassedPayload passedPayload = new PassedPayload(payload, testRun);
                testRun.getPassedPayloads().add(passedPayload);
            
                logger.debug("Payload passed: {}", payload.length() > 100 ? payload.substring(0, 97) + "..." : payload);

            } catch (HttpClientErrorException.Forbidden e) {
                // This is the success case: the WAF blocked a malicious request (HTTP 403)
                // Removed testModeService.recordBlockedRequest() to fix double-counting
                testRun.setTotalBlocked(testRun.getTotalBlocked() + 1);
                testRun.setTotalMaliciousBlocked(testRun.getTotalMaliciousBlocked() + 1);
            
                logger.debug("Payload blocked: {}", payload.length() > 100 ? payload.substring(0, 97) + "..." : payload);

            } catch (HttpClientErrorException e) {
                // Any other 4xx error from the client means the WAF let it through.
                // We count this as a "passed" request and log it for debugging.
                logger.warn("Test request passed WAF but resulted in a client error ({}): {}", 
                        e.getStatusCode(), e.getResponseBodyAsString());
                // Removed testModeService.recordPassedRequest() to fix double-counting
                testRun.setTotalPassed(testRun.getTotalPassed() + 1);
                PassedPayload passedPayload = new PassedPayload(payload, testRun);
                testRun.getPassedPayloads().add(passedPayload);

            } catch (Exception e) {
                // Other, more serious errors (e.g., connection refused, 5xx server errors).
                // We'll log them but still count them as passed so the test can complete.
                logger.error("Unexpected error sending payload in test: {}", e.getMessage());
                // Removed testModeService.recordPassedRequest() to fix double-counting
                testRun.setTotalPassed(testRun.getTotalPassed() + 1);
                PassedPayload passedPayload = new PassedPayload(payload, testRun);
                testRun.getPassedPayloads().add(passedPayload);
            }
        }
    }

    /**
     * Processes each HTTP request by sending it with the appropriate method and updates the test run
     * @param requests The list of HTTP requests to process
     * @param testRun The test run to update
     */
    private void processRequests(List<HttpRequestData> requests, TestRun testRun) {
        String baseUrl = "http://localhost:8080/api/test/test-harness"; // Correct endpoint that matches our controller
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
    
        logger.info("Processing {} HTTP requests by sending them as simulated HTTP requests...", requests.size());
        logger.info("[DEBUG_LOG] processRequests received {} requests to process", requests.size());
        int processedCount = 0;
    
        // Process each request
        for (HttpRequestData requestData : requests) {
            String method = requestData.getMethod();
            String payload = requestData.getPayload();
        
            try {
                // Build the URL with the payload as query parameters
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
            
                // If the payload contains query parameters (e.g., key=value&key2=value2), add them to the URL
                if (payload != null && !payload.isEmpty()) {
                    // The payload might already be in the form of query parameters
                    // We'll append it directly to the URL without parsing
                    String url = baseUrl;
                    if (!url.contains("?")) {
                        url += "?";
                    } else if (!url.endsWith("?")) {
                        url += "&";
                    }
                    url += payload;
                
                    // Log the full URL for debugging
                    logger.debug("Sending {} request to: {}", method, url);
                
                    // Send the request with the appropriate method
                    if ("GET".equalsIgnoreCase(method)) {
                        restTemplate.getForEntity(url, String.class);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        // For POST, we send an empty body but include the payload in the URL
                        HttpEntity<String> request = new HttpEntity<>("", headers);
                        restTemplate.postForEntity(url, request, String.class);
                    } else {
                        // For other methods, use exchange
                        HttpEntity<String> request = new HttpEntity<>("", headers);
                        restTemplate.exchange(url, HttpMethod.valueOf(method.toUpperCase()), request, String.class);
                    }
                } else {
                    // If there's no payload, just send the request with the appropriate method
                    if ("GET".equalsIgnoreCase(method)) {
                        restTemplate.getForEntity(baseUrl, String.class);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        HttpEntity<String> request = new HttpEntity<>("", headers);
                        restTemplate.postForEntity(baseUrl, request, String.class);
                    } else {
                        HttpEntity<String> request = new HttpEntity<>("", headers);
                        restTemplate.exchange(baseUrl, HttpMethod.valueOf(method.toUpperCase()), request, String.class);
                    }
                }
            
                // If we get here, the WAF did NOT block it (it passed)
                // Removed testModeService.recordPassedRequest() to fix double-counting
                testRun.setTotalPassed(testRun.getTotalPassed() + 1);
                PassedPayload passedPayload = new PassedPayload(payload, testRun);
                testRun.getPassedPayloads().add(passedPayload);
                // Append to transient buffer for post-run LLM batching
                synchronized (lastPassedPayloadsForLlm) {
                    lastPassedPayloadsForLlm.add(payload);
                }
            
                logger.debug("Request passed: {} {}", method, payload.length() > 100 ? payload.substring(0, 97) + "..." : payload);

            } catch (HttpClientErrorException.Forbidden e) {
                // This is the success case: the WAF blocked a malicious request (HTTP 403)
                // Removed testModeService.recordBlockedRequest() to fix double-counting
                testRun.setTotalBlocked(testRun.getTotalBlocked() + 1);
                testRun.setTotalMaliciousBlocked(testRun.getTotalMaliciousBlocked() + 1);
            
                logger.debug("Request blocked: {} {}", method, payload.length() > 100 ? payload.substring(0, 97) + "..." : payload);

            } catch (HttpClientErrorException e) {
                // Any other 4xx error from the client means the WAF let it through.
                // We count this as a "passed" request and log it for debugging.
                logger.warn("Test request passed WAF but resulted in a client error ({}): {}", 
                        e.getStatusCode(), e.getResponseBodyAsString());
                // Removed testModeService.recordPassedRequest() to fix double-counting
                testRun.setTotalPassed(testRun.getTotalPassed() + 1);
                PassedPayload passedPayload = new PassedPayload(payload, testRun);
                testRun.getPassedPayloads().add(passedPayload);
                synchronized (lastPassedPayloadsForLlm) {
                    lastPassedPayloadsForLlm.add(payload);
                }

            } catch (Exception e) {
                // Other, more serious errors (e.g., connection refused, 5xx server errors).
                // We'll log them but still count them as passed so the test can complete.
                logger.error("Unexpected error sending request in test: {}", e.getMessage());
                // Removed testModeService.recordPassedRequest() to fix double-counting
                testRun.setTotalPassed(testRun.getTotalPassed() + 1);
                PassedPayload passedPayload = new PassedPayload(payload, testRun);
                testRun.getPassedPayloads().add(passedPayload);
                synchronized (lastPassedPayloadsForLlm) {
                    lastPassedPayloadsForLlm.add(payload);
                }
            }
            
            processedCount++;
            if (processedCount % 100 == 0) {
                logger.info("[DEBUG_LOG] Processed {} requests so far", processedCount);
            }
        }
        
        logger.info("[DEBUG_LOG] Total requests processed: {}", processedCount);
    }
}