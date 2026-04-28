package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {
    List<DailyActivity> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT d FROM DailyActivity d WHERE d.user.id = :userId")
    List<DailyActivity> findByUserId(@Param("userId") Long userId);

    @Query("SELECT d FROM DailyActivity d WHERE d.user.id = :userId AND d.date BETWEEN :startDate AND :endDate")
    List<DailyActivity> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}