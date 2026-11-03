package com.smartschedule.repository;

import com.smartschedule.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {
    List<DailyTask> findByDailyPlanIdOrderBySortOrderAsc(Long dailyPlanId);
    List<DailyTask> findByGoalId(Long goalId);
}
