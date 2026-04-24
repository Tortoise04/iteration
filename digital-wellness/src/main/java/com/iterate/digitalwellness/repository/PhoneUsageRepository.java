package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.PhoneUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PhoneUsageRepository extends JpaRepository<PhoneUsage, Long> {
    List<PhoneUsage> findByDateBetween(LocalDate startDate, LocalDate endDate);
}