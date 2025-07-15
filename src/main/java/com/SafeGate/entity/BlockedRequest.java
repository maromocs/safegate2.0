package com.SafeGate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "blocked_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(name = "source_ip", nullable = false)
    private String sourceIp;
    
    @Column(name = "matched_pattern", nullable = false)
    private String matchedPattern;
    
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
    
    @Column(name = "rule_id")
    private String ruleId;
    
    @Column(name = "request_method")
    private String requestMethod;
    
    @Column(name = "request_uri")
    private String requestUri;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
    
    public BlockedRequest(String sourceIp, String matchedPattern, String rawPayload, String ruleId) {
        this.sourceIp = sourceIp;
        this.matchedPattern = matchedPattern;
        this.rawPayload = rawPayload;
        this.ruleId = ruleId;
        this.timestamp = Instant.now();
    }
}