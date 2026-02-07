package com.SafeGate.controller;

import com.SafeGate.entity.BlockedRequest;
import com.SafeGate.repository.BlockedRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityController {
    
    @Autowired
    private BlockedRequestRepository blockedRequestRepository;
    
    @GetMapping("/blocked")
    public List<BlockedRequest> getBlockedRequests() {
        return blockedRequestRepository.findAllByOrderByTimestampDesc();
    }
    
    @GetMapping("/blocked/count")
    public long getBlockedCount() {
        return blockedRequestRepository.count();
    }
}