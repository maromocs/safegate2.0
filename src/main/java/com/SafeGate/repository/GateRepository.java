package com.SafeGate.repository;

import com.SafeGate.entity.GateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GateRepository extends JpaRepository<GateEntity, Long> {
    Optional<GateEntity> findByName(String name);
    boolean existsByName(String name);
}