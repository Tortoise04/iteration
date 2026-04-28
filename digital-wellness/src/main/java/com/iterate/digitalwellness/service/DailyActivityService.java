package com.iterate.digitalwellness.service;

import com.iterate.digitalwellness.entity.DailyActivity;

import java.time.LocalDate;
import java.util.List;

public interface DailyActivityService {
    DailyActivity save(DailyActivity dailyActivity);
    List<DailyActivity> findAll();
    List<DailyActivity> findByUserId(Long userId);
    DailyActivity findById(Long id);
    void deleteById(Long id);
    List<DailyActivity> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<DailyActivity> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}