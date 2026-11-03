package com.smartschedule.repository;

import com.smartschedule.entity.PointsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {
    List<PointsRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
