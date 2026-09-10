package com.eldercare.repo;

import com.eldercare.domain.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findByApplicationId(Long applicationId);
}
