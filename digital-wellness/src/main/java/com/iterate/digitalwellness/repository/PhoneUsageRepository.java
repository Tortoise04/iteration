package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.PhoneUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PhoneUsageRepository extends JpaRepository<PhoneUsage, Long> {
    List<PhoneUsage> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT p FROM PhoneUsage p WHERE p.user.id = :userId")
    List<PhoneUsage> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM PhoneUsage p WHERE p.user.id = :userId AND p.date BETWEEN :startDate AND :endDate")
    List<PhoneUsage> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
