package com.eldercare.repo;

import com.eldercare.domain.PlanConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanConfirmationRepository extends JpaRepository<PlanConfirmation, Long> {
    List<PlanConfirmation> findByApplicationIdOrderByRoundNoDesc(Long applicationId);
    Optional<PlanConfirmation> findFirstByApplicationIdOrderByRoundNoDesc(Long applicationId);
}
