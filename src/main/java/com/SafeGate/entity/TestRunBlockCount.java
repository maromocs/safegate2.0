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

    private String ruleName;
    private long count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")
    @JsonIgnore // Avoid circular dependency in JSON serialization
    private TestRun testRun;

    public TestRunBlockCount(String ruleName, long count, TestRun testRun) {
        this.ruleName = ruleName;
        this.count = count;
        this.testRun = testRun;
    }
}