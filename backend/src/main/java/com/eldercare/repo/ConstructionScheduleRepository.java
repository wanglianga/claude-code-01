package com.eldercare.repo;

import com.eldercare.domain.ConstructionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConstructionScheduleRepository extends JpaRepository<ConstructionSchedule, Long> {
    Optional<ConstructionSchedule> findByApplicationId(Long applicationId);
    List<ConstructionSchedule> findByTeamId(Long teamId);
}
