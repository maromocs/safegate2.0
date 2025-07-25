package com.SafeGate.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_runs")
@Data
@NoArgsConstructor
public class TestRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long totalPassed = 0;
    private long totalBlocked = 0;
    
    // New fields for dataset testing
    private String datasetFileName;
    private String datasetFormat;
    private String attackTypeTag;
    private String samplingSize;
    
    // Fields for malicious attack analysis
    private long totalMaliciousRequests = 0;
    private long totalMaliciousBlocked = 0;
    
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TestRunBlockCount> blockCounts = new ArrayList<>();
    
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PassedPayload> passedPayloads = new ArrayList<>();
    
    // Helper method to get test parameters as a string
    @Transient
    public String getTestParameters() {
        if (datasetFileName == null) {
            return "Manual Test";
        }
        
        StringBuilder params = new StringBuilder();
        if (attackTypeTag != null && !attackTypeTag.isEmpty()) {
            params.append("Type: ").append(attackTypeTag);
        }
        
        if (samplingSize != null && !samplingSize.isEmpty()) {
            if (params.length() > 0) {
                params.append(", ");
            }
            params.append("Sample: ").append(samplingSize);
        }
        
        if (params.length() == 0) {
            return "Dataset Test";
        }
        
        return params.toString();
    }
    
    // Helper method to calculate effectiveness percentage
    @Transient
    public double getEffectivenessPercentage() {
        if (totalMaliciousRequests == 0) {
            return 0.0;
        }
        return (double) totalMaliciousBlocked / totalMaliciousRequests * 100.0;
    }
}