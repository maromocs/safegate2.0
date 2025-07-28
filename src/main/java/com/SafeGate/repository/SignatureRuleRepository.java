package com.SafeGate.repository;

import com.SafeGate.model.SignatureRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SignatureRuleRepository extends JpaRepository<SignatureRule, Long> {
    List<SignatureRule> findAllByEnabledTrue();
}