package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.PeriodSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeriodSummaryRepository extends JpaRepository<PeriodSummary, Long> {

    @Query("SELECT p FROM PeriodSummary p WHERE p.user.id = :userId")
    List<PeriodSummary> findByUserId(@Param("userId") Long userId);
}