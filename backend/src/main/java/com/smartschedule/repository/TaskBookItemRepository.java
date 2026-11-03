package com.smartschedule.repository;

import com.smartschedule.entity.TaskBookItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskBookItemRepository extends JpaRepository<TaskBookItem, Long> {
    List<TaskBookItem> findByBookIdOrderByPlanDateAscSortOrderAsc(Long bookId);
    List<TaskBookItem> findByBookIdInAndPlanDate(List<Long> bookIds, LocalDate planDate);
    List<TaskBookItem> findByBookIdInAndStatusAndPlanDateLessThan(List<Long> bookIds, String status, LocalDate planDate);
    List<TaskBookItem> findByBookIdIn(List<Long> bookIds);
    void deleteByBookId(Long bookId);
}
