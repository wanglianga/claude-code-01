package com.eldercare.repo;

import com.eldercare.domain.SubsidyReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubsidyReviewRepository extends JpaRepository<SubsidyReview, Long> {
    Optional<SubsidyReview> findByApplicationId(Long applicationId);
}
