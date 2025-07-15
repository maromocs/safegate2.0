package com.SafeGate.repository;

import com.SafeGate.entity.BlockedRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BlockedRequestRepository extends JpaRepository<BlockedRequest, Long> {
    
    List<BlockedRequest> findAllByOrderByTimestampDesc();
    List<BlockedRequest> findByMatchedPatternOrderByTimestampDesc(String matchedPattern);
    List<BlockedRequest> findBySourceIpOrderByTimestampDesc(String sourceIp);
    
    @Query("SELECT b FROM BlockedRequest b WHERE b.timestamp >= :since ORDER BY b.timestamp DESC")
    List<BlockedRequest> findRecentRequests(@Param("since") Instant since);
    
    @Query("SELECT b.matchedPattern, COUNT(b) FROM BlockedRequest b GROUP BY b.matchedPattern ORDER BY COUNT(b) DESC")
    List<Object[]> findAttackPatternStats();
    
    @Query("SELECT b.sourceIp, COUNT(b) FROM BlockedRequest b GROUP BY b.sourceIp ORDER BY COUNT(b) DESC")
    List<Object[]> findTopAttackingIPs();
}