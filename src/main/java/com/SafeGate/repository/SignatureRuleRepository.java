package com.SafeGate.repository;

import com.SafeGate.model.SignatureRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignatureRuleRepository extends JpaRepository<SignatureRule, Long> {
}