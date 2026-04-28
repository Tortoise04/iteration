package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.DailySubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailySubtaskRepository extends JpaRepository<DailySubtask, Long> {
    List<DailySubtask> findByGoalId(Long goalId);
    
    @Query("SELECT ds FROM DailySubtask ds WHERE ds.goal.user.id = :userId AND ds.targetDate = :targetDate")
    List<DailySubtask> findByUserIdAndTargetDate(@Param("userId") Long userId, @Param("targetDate") LocalDate targetDate);
    
    @Query("SELECT ds FROM DailySubtask ds WHERE ds.goal.user.id = :userId AND ds.targetDate BETWEEN :startDate AND :endDate")
    List<DailySubtask> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
