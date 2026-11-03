package com.smartschedule.repository;

import com.smartschedule.entity.DailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {
    Optional<DailyPlan> findByUserIdAndPlanDate(Long userId, LocalDate planDate);
    List<DailyPlan> findByUserIdOrderByPlanDateDesc(Long userId);
}
