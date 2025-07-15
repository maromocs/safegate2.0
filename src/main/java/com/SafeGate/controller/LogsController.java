package com.SafeGate.controller;

import com.SafeGate.entity.BlockedRequest;
import com.SafeGate.repository.BlockedRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LogsController {
    
    @Autowired
    private BlockedRequestRepository blockedRequestRepository;
    
    @GetMapping("/logs")
    public List<BlockedRequest> getLogs() {
        return blockedRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }
    
    @GetMapping("/logs/recent")
    public List<BlockedRequest> getRecentLogs(@RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return blockedRequestRepository.findRecentRequests(since);
    }
    
    @GetMapping("/logs/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalBlocked", blockedRequestRepository.count());
        
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        stats.put("blockedLast24h", blockedRequestRepository.findRecentRequests(since24h).size());
        
        stats.put("attackPatterns", blockedRequestRepository.findAttackPatternStats());
        stats.put("topAttackingIPs", blockedRequestRepository.findTopAttackingIPs());
        stats.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(stats);
    }
    
    @DeleteMapping("/logs")
    public ResponseEntity<Map<String, String>> clearLogs() {
        blockedRequestRepository.deleteAll();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All logs cleared");
        return ResponseEntity.ok(response);
    }
}