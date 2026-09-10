package com.eldercare.repo;

import com.eldercare.domain.ChangeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeItemRepository extends JpaRepository<ChangeItem, Long> {
    List<ChangeItem> findByChangeId(Long changeId);
    List<ChangeItem> findByApplicationId(Long applicationId);
}
