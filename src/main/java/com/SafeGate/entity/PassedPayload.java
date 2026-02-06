package com.SafeGate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "passed_payloads")
@Data
@NoArgsConstructor
public class PassedPayload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10000)
    private String payload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")
    @JsonIgnore // Avoid circular dependency in JSON serialization
    private TestRun testRun;

    public PassedPayload(String payload, TestRun testRun) {
        this.payload = payload;
        this.testRun = testRun;
    }
}