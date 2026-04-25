package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    /**
     * 查询在指定时间范围内活跃的目标
     * (目标开始时间 <= 结束时间 AND 目标结束时间 >= 开始时间)
     */
    @Query("SELECT g FROM Goal g WHERE g.startTime <= :endDate AND g.endTime >= :startDate")
    List<Goal> findActiveGoalsInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 按状态查询目标
     */
    List<Goal> findByStatus(String status);
}
