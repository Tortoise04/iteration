package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.PeriodSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodSummaryRepository extends JpaRepository<PeriodSummary, Long> {
}