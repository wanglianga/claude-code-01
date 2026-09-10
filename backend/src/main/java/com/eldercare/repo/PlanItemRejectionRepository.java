package com.eldercare.repo;

import com.eldercare.domain.PlanItemRejection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanItemRejectionRepository extends JpaRepository<PlanItemRejection, Long> {
    List<PlanItemRejection> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
