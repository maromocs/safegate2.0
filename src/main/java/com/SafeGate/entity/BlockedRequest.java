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
    private String detectionCategory;
    
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
    
    public BlockedRequest(String sourceIp, String detectionCategory, String rawPayload, String ruleId) {
        this.sourceIp = sourceIp;
        this.detectionCategory = detectionCategory;
        this.rawPayload = rawPayload;
        this.ruleId = ruleId;
        this.timestamp = Instant.now();
    }

    // Manual getters/setters to fix build issues when Lombok fails
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getDetectionCategory() { return detectionCategory; }
    public void setDetectionCategory(String detectionCategory) { this.detectionCategory = detectionCategory; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}