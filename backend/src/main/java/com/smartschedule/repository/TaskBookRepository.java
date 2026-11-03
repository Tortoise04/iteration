package com.smartschedule.repository;

import com.smartschedule.entity.TaskBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskBookRepository extends JpaRepository<TaskBook, Long> {
    List<TaskBook> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<TaskBook> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<String> statuses);
}
