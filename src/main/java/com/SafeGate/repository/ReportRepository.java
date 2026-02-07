package com.SafeGate.repository;

import com.SafeGate.entity.Report;
import com.SafeGate.entity.User;
import com.SafeGate.enums.ReportStatus;
import com.SafeGate.enums.ReportSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReporter(User reporter);
    List<Report> findByStatus(ReportStatus status);
    List<Report> findBySeverity(ReportSeverity severity);
    List<Report> findByTitleContainingIgnoreCase(String title);
}