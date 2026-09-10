package com.eldercare.repo;

import com.eldercare.domain.ConstructionChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConstructionChangeRepository extends JpaRepository<ConstructionChange, Long> {
    List<ConstructionChange> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
