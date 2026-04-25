package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.AppUsageDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppUsageDetailRepository extends JpaRepository<AppUsageDetail, Long> {
    List<AppUsageDetail> findByPhoneUsageId(Long phoneUsageId);
}
