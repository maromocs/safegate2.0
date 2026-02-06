package com.SafeGate.controller;

import com.SafeGate.entity.PassedPayload;
import com.SafeGate.entity.TestRun;
import com.SafeGate.repository.TestRunRepository;
import com.SafeGate.service.DatasetTestRunnerService;
import com.SafeGate.service.LLMService;
import com.SafeGate.service.WafTestModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/tests", "/api/test"})
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private WafTestModeService testModeService;

    @Autowired
    private TestRunRepository testRunRepository;
    
    @Autowired
    private DatasetTestRunnerService datasetTestRunnerService;

    @Autowired
    private LLMService llmService;

    @PostMapping("/start")
    public ResponseEntity<?> startTest() {
        try {
            testModeService.startTest();
            return ResponseEntity.ok().body(Map.of("message", "Test started successfully."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopTest() {
        try {
            TestRun completedTest = testModeService.stopTest();
            return ResponseEntity.ok(completedTest);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("testModeEnabled", testModeService.isTestModeEnabled());
        Optional<TestRun> currentRunState = testModeService.getCurrentTestRunState();
        currentRunState.ifPresent(testRun -> status.put("currentRun", testRun));
        return ResponseEntity.ok(status);
    }

    @GetMapping("/results")
    public ResponseEntity<List<TestRun>> getAllResults() {
        return ResponseEntity.ok(testRunRepository.findAll());
    }
    
    /**
     * Delete a test run by ID
     * 
     * @param id The ID of the test run to delete
     * @return A success message or an error response
     */
    @DeleteMapping("/results/{id}")
    public ResponseEntity<?> deleteTestRun(@PathVariable Long id) {
        try {
            if (!testRunRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            testRunRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Test run deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error deleting test run: " + e.getMessage()));
        }
    }
    
    /**
     * Start a dataset test with the provided file and parameters.
     * If datasetFormat is not specified, the format will be auto-detected.
     * 
     * @param file The dataset file to test
     * @param datasetFormat The format of the dataset (TXT, CSV, JSON, XML, AUTO). Optional, defaults to AUTO.
     * @param attackTypeTag Optional tag for the attack type
     * @param samplingSize Number of attacks to test (All, Random 100, Random 1000, etc.)
     * @return The completed test run or an error response
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/start-dataset-test")
    public ResponseEntity<?> startDatasetTest(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "datasetFormat", required = false, defaultValue = "AUTO") String datasetFormat,
            @RequestParam(value = "attackTypeTag", required = false) String attackTypeTag,
            @RequestParam(value = "samplingSize", required = false, defaultValue = "All") String samplingSize,
            @RequestParam(value = "seed", required = false) Long seed) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Run the dataset test
            TestRun completedTest = datasetTestRunnerService.runDatasetTest(file, datasetFormat, attackTypeTag, samplingSize, seed);
            
            // Add the detected format to the response
            result.put("testRun", completedTest);
            result.put("detectedFormat", completedTest.getDatasetFormat());
            result.put("message", "Test completed successfully using " + completedTest.getDatasetFormat() + " format");
            
            // If LLM mode is active for dataset, analyze passed payloads via batch API
            if (llmService.isLlmActiveForDataset()) {
                // Prefer transient buffer first to avoid JPA lazy/timing issues
                List<String> payloads = datasetTestRunnerService.consumePassedPayloadsForLlm();
                boolean usedFallback = false;
                if (payloads.isEmpty() && completedTest.getPassedPayloads() != null && !completedTest.getPassedPayloads().isEmpty()) {
                    usedFallback = true;
                    for (PassedPayload p : completedTest.getPassedPayloads()) {
                        if (p.getPayload() != null) payloads.add(p.getPayload());
                    }
                }
                if (payloads.isEmpty()) {
                    logger.info("[LLM][DATASET] Skipping batch analysis: no passed payloads to analyze (buffer and entity empty).");
                } else {
                    String provider = llmService.getConfig().map(cfg -> String.valueOf(cfg.getProvider())).orElse("?");
                    String model = llmService.getConfig().map(cfg -> String.valueOf(cfg.getModel())).orElse("?");
                    logger.info("[LLM][DATASET] Starting batch analysis for {} payloads (fallbackUsed={}) using provider={} model={}", payloads.size(), usedFallback, provider, model);

                    Map batchResponse = llmService.analyzeBatch(payloads);
                    Object statsObj = batchResponse.get("stats");
                    Map stats = statsObj instanceof Map ? (Map) statsObj : Map.of();
                    Object total = stats.getOrDefault("total", 0);
                    Object malicious = stats.getOrDefault("malicious", 0);
                    Object safe = stats.getOrDefault("safe", 0);
                    Object byCategory = stats.getOrDefault("byCategory", Map.of());
                    logger.info("[LLM][DATASET] Completed. Totals: total={} malicious={} safe={} byCategory={}", total, malicious, safe, byCategory);

                    // Log up to 3 malicious examples
                    int examples = 0;
                    Object resultsObj = batchResponse.get("results");
                    if (resultsObj instanceof List) {
                        List results = (List) resultsObj;
                        for (Object item : results) {
                            if (item instanceof Map && examples < 3) {
                                Map itemMap = (Map) item;
                                Object mal = itemMap.get("is_malicious");
                                if (mal instanceof Boolean && (Boolean) mal) {
                                    String payload = String.valueOf(itemMap.get("payload"));
                                    String category = String.valueOf(itemMap.getOrDefault("category", "OTHER"));
                                    String reason = String.valueOf(itemMap.getOrDefault("reason", ""));
                                    if (payload.length() > 120) payload = payload.substring(0, 117) + "...";
                                    logger.info("[LLM][DATASET] Malicious example [{}]: category={} reason={} payload={} ", examples + 1, category, reason, payload);
                                    examples++;
                                }
                            }
                        }
                    }

                    result.put("llmStats", stats);
                    // Build lists of malicious and safe payloads for UI convenience
                    List<Map<String, Object>> maliciousList = new ArrayList<>();
                    List<Map<String, Object>> safeList = new ArrayList<>();
                    if (resultsObj instanceof List) {
                        List results = (List) resultsObj;
                        for (Object item : results) {
                            if (item instanceof Map) {
                                Map itemMap = (Map) item;
                                Object mal = itemMap.get("is_malicious");
                                boolean isMalicious = (mal instanceof Boolean) && (Boolean) mal;
                                Map<String, Object> row = new HashMap<>();
                                row.put("payload", itemMap.get("payload"));
                                row.put("category", itemMap.get("category"));
                                row.put("reason", itemMap.get("reason"));
                                row.put("is_malicious", isMalicious);
                                if (isMalicious) {
                                    maliciousList.add(row);
                                } else {
                                    safeList.add(row);
                                }
                            }
                        }
                    }
                    result.put("llmMaliciousPayloads", maliciousList);
                    result.put("llmSafePayloads", safeList);
                }
            } else {
                String mode = llmService.getConfig().map(cfg -> String.valueOf(cfg.getLlmMode())).orElse("DISABLED");
                logger.info("[LLM][DATASET] Dataset LLM analysis is disabled. Mode={}", mode);
            }
            
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorType", "IllegalStateException"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorType", "IllegalArgumentException",
                "suggestion", "Try specifying the format explicitly or check if the file content is valid"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error processing dataset: " + e.getMessage(),
                "errorType", e.getClass().getSimpleName(),
                "suggestion", "Check the file format and content"
            ));
        }
    }
    
    /**
     * Test harness endpoint for WAF dataset tests.
     * This endpoint exists solely to act as a target for the WAF dataset tests.
     * If a request reaches this point, it means the WAF did not block it.
     * 
     * @param payload The payload sent in the request body
     * @return A 200 OK response indicating the payload was received
     */
    @PostMapping("/test-harness")
    public ResponseEntity<String> testHarness(@RequestBody(required = false) String payload) {
        // Simply return a 200 OK response
        return ResponseEntity.ok("Payload received by test harness.");
    }
}