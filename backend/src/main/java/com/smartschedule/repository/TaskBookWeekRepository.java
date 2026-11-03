package com.smartschedule.repository;

import com.smartschedule.entity.TaskBookWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskBookWeekRepository extends JpaRepository<TaskBookWeek, Long> {
    List<TaskBookWeek> findByBookIdOrderByWeekIndexAsc(Long bookId);
    void deleteByBookId(Long bookId);
}
