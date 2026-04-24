package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {
    List<DailyActivity> findByDateBetween(LocalDate startDate, LocalDate endDate);
}