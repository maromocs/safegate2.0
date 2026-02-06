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
    private Long seedNumber;
    
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
        
        if (seedNumber != null) {
            if (params.length() > 0) {
                params.append(", ");
            }
            params.append("Seed: ").append(seedNumber);
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

    // Manual getters/setters to fix build issues when Lombok fails
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public long getTotalPassed() { return totalPassed; }
    public void setTotalPassed(long totalPassed) { this.totalPassed = totalPassed; }
    public long getTotalBlocked() { return totalBlocked; }
    public void setTotalBlocked(long totalBlocked) { this.totalBlocked = totalBlocked; }
    public String getDatasetFileName() { return datasetFileName; }
    public void setDatasetFileName(String datasetFileName) { this.datasetFileName = datasetFileName; }
    public String getDatasetFormat() { return datasetFormat; }
    public void setDatasetFormat(String datasetFormat) { this.datasetFormat = datasetFormat; }
    public String getAttackTypeTag() { return attackTypeTag; }
    public void setAttackTypeTag(String attackTypeTag) { this.attackTypeTag = attackTypeTag; }
    public String getSamplingSize() { return samplingSize; }
    public void setSamplingSize(String samplingSize) { this.samplingSize = samplingSize; }
    public Long getSeedNumber() { return seedNumber; }
    public void setSeedNumber(Long seedNumber) { this.seedNumber = seedNumber; }
    public long getTotalMaliciousRequests() { return totalMaliciousRequests; }
    public void setTotalMaliciousRequests(long totalMaliciousRequests) { this.totalMaliciousRequests = totalMaliciousRequests; }
    public long getTotalMaliciousBlocked() { return totalMaliciousBlocked; }
    public void setTotalMaliciousBlocked(long totalMaliciousBlocked) { this.totalMaliciousBlocked = totalMaliciousBlocked; }
    public List<TestRunBlockCount> getBlockCounts() { return blockCounts; }
    public void setBlockCounts(List<TestRunBlockCount> blockCounts) { this.blockCounts = blockCounts; }
    public List<PassedPayload> getPassedPayloads() { return passedPayloads; }
    public void setPassedPayloads(List<PassedPayload> passedPayloads) { this.passedPayloads = passedPayloads; }
}