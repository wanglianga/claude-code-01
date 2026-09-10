package com.eldercare.repo;

import com.eldercare.domain.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {
    List<PlanItem> findByApplicationId(Long applicationId);
    List<PlanItem> findByApplicationIdAndStatusNot(Long applicationId, String status);
}
