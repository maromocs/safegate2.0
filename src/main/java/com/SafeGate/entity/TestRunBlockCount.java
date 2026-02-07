package com.SafeGate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_run_block_counts")
@Data
@NoArgsConstructor
public class TestRunBlockCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String detectionCategory;
    private long count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")
    @JsonIgnore // Avoid circular dependency in JSON serialization
    private TestRun testRun;

    public TestRunBlockCount(String detectionCategory, long count, TestRun testRun) {
        this.detectionCategory = detectionCategory;
        this.count = count;
        this.testRun = testRun;
    }

    // Manual getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDetectionCategory() { return detectionCategory; }
    public void setDetectionCategory(String detectionCategory) { this.detectionCategory = detectionCategory; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public TestRun getTestRun() { return testRun; }
    public void setTestRun(TestRun testRun) { this.testRun = testRun; }
}