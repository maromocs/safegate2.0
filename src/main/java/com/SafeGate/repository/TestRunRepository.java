package com.SafeGate.repository;

import com.SafeGate.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {}