package com.SafeGate.service;

import com.SafeGate.entity.TestRun;
import com.SafeGate.entity.TestRunBlockCount;
import com.SafeGate.repository.TestRunRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Getter
public class WafTestModeService {

    @Autowired
    private TestRunRepository testRunRepository;

    private boolean testModeEnabled = false;
    private final AtomicLong passedRequests = new AtomicLong(0);
    private final Map<String, AtomicLong> blockedRequestCounts = new ConcurrentHashMap<>();
    private TestRun currentTestRun;

    public synchronized void startTest() {
        if (testModeEnabled) {
            throw new IllegalStateException("A test is already in progress.");
        }
        testModeEnabled = true;
        passedRequests.set(0);
        blockedRequestCounts.clear();

        currentTestRun = new TestRun();
        currentTestRun.setStartTime(LocalDateTime.now());
    }

    public synchronized TestRun stopTest() {
        if (!testModeEnabled) {
            throw new IllegalStateException("No test is currently in progress.");
        }
        testModeEnabled = false;
        currentTestRun.setEndTime(LocalDateTime.now());
        currentTestRun.setTotalPassed(passedRequests.get());

        long totalBlocked = 0;
        for (Map.Entry<String, AtomicLong> entry : blockedRequestCounts.entrySet()) {
            long count = entry.getValue().get();
            totalBlocked += count;
            TestRunBlockCount blockCount = new TestRunBlockCount(entry.getKey(), count, currentTestRun);
            currentTestRun.getBlockCounts().add(blockCount);
        }
        currentTestRun.setTotalBlocked(totalBlocked);

        return testRunRepository.save(currentTestRun);
    }

    public void recordPassedRequest() {
        if (testModeEnabled) {
            passedRequests.incrementAndGet();
        }
    }

    public void recordBlockedRequest(String detectionCategory) {
        if (testModeEnabled) {
            blockedRequestCounts.computeIfAbsent(detectionCategory, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public Optional<TestRun> getCurrentTestRunState() {
        if (!testModeEnabled || currentTestRun == null) {
            return Optional.empty();
        }
        // Create a snapshot for returning
        TestRun snapshot = new TestRun();
        snapshot.setStartTime(currentTestRun.getStartTime());
        snapshot.setTotalPassed(passedRequests.get());
        long totalBlocked = 0;
        for (Map.Entry<String, AtomicLong> entry : blockedRequestCounts.entrySet()) {
            long count = entry.getValue().get();
            totalBlocked += count;
            snapshot.getBlockCounts().add(new TestRunBlockCount(entry.getKey(), count, snapshot));
        }
        snapshot.setTotalBlocked(totalBlocked);
        return Optional.of(snapshot);
    }

    public boolean isTestModeEnabled() { return testModeEnabled; }
    public TestRun getCurrentTestRun() { return currentTestRun; }
}