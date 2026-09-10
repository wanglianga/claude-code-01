package com.eldercare.repo;

import com.eldercare.domain.WorkflowLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowLogRepository extends JpaRepository<WorkflowLog, Long> {
    List<WorkflowLog> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
