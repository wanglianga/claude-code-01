package com.eldercare.repo;

import com.eldercare.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByApplicationId(Long applicationId);
}
