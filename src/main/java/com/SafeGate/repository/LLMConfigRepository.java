package com.SafeGate.repository;

import com.SafeGate.model.LLMConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LLMConfigRepository extends JpaRepository<LLMConfig, Long> {
}