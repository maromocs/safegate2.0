package com.SafeGate.controller;

import com.SafeGate.entity.TestRun;
import com.SafeGate.repository.TestRunRepository;
import com.SafeGate.service.DatasetTestRunnerService;
import com.SafeGate.service.WafTestModeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/tests", "/api/test"})
public class TestController {

    @Autowired
    private WafTestModeService testModeService;

    @Autowired
    private TestRunRepository testRunRepository;
    
    @Autowired
    private DatasetTestRunnerService datasetTestRunnerService;

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
    @PostMapping("/start-dataset-test")
    public ResponseEntity<?> startDatasetTest(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "datasetFormat", required = false, defaultValue = "AUTO") String datasetFormat,
            @RequestParam(value = "attackTypeTag", required = false) String attackTypeTag,
            @RequestParam(value = "samplingSize", required = false, defaultValue = "All") String samplingSize) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Run the dataset test
            TestRun completedTest = datasetTestRunnerService.runDatasetTest(file, datasetFormat, attackTypeTag, samplingSize);
            
            // Add the detected format to the response
            result.put("testRun", completedTest);
            result.put("detectedFormat", completedTest.getDatasetFormat());
            result.put("message", "Test completed successfully using " + completedTest.getDatasetFormat() + " format");
            
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