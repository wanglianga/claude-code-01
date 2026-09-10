package com.eldercare.repo;

import com.eldercare.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStatusOrderBySubmittedAtDesc(String status);
    List<Application> findAllByOrderBySubmittedAtDesc();
    List<Application> findByApplicantUserIdOrderBySubmittedAtDesc(Long applicantUserId);
    List<Application> findByAssessorIdOrderByAssignedAtDesc(Long assessorId);
}
