package com.eldercare.repo;

import com.eldercare.domain.WarrantyVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarrantyVisitRepository extends JpaRepository<WarrantyVisit, Long> {
    List<WarrantyVisit> findByApplicationIdOrderByVisitTimeDesc(Long applicationId);
}
