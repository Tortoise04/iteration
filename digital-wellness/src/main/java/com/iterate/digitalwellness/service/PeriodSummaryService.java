package com.iterate.digitalwellness.service;

import com.iterate.digitalwellness.entity.PeriodSummary;

import java.time.LocalDate;
import java.util.List;

public interface PeriodSummaryService {
    PeriodSummary save(PeriodSummary periodSummary);
    List<PeriodSummary> findAll();
    List<PeriodSummary> findByUserId(Long userId);
    PeriodSummary findById(Long id);
    void deleteById(Long id);
    PeriodSummary generateSummary(LocalDate startDate, LocalDate endDate, String periodType);
    PeriodSummary generateSummary(Long userId, LocalDate startDate, LocalDate endDate, String periodType);
}