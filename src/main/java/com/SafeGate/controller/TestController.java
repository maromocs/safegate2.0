package com.SafeGate.controller;

import com.SafeGate.entity.TestRun;
import com.SafeGate.repository.TestRunRepository;
import com.SafeGate.service.WafTestModeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    @Autowired
    private WafTestModeService testModeService;

    @Autowired
    private TestRunRepository testRunRepository;

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
}