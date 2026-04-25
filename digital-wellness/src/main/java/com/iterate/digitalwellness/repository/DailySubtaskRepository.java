package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.DailySubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailySubtaskRepository extends JpaRepository<DailySubtask, Long> {
    List<DailySubtask> findByGoalId(Long goalId);
    List<DailySubtask> findByGoalIdAndIsCompleted(Long goalId, Boolean isCompleted);
}
