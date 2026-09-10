package com.eldercare.repo;

import com.eldercare.domain.Completion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompletionRepository extends JpaRepository<Completion, Long> {
    Optional<Completion> findByApplicationId(Long applicationId);
}
