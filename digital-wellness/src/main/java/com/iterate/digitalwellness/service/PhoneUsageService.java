package com.iterate.digitalwellness.service;

import com.iterate.digitalwellness.entity.PhoneUsage;

import java.time.LocalDate;
import java.util.List;

public interface PhoneUsageService {
    PhoneUsage save(PhoneUsage phoneUsage);
    List<PhoneUsage> findAll();
    List<PhoneUsage> findByUserId(Long userId);
    PhoneUsage findById(Long id);
    void deleteById(Long id);
    List<PhoneUsage> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<PhoneUsage> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
