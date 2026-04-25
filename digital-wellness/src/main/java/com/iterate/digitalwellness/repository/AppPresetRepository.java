package com.iterate.digitalwellness.repository;

import com.iterate.digitalwellness.entity.AppPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppPresetRepository extends JpaRepository<AppPreset, Long> {
    List<AppPreset> findByIsActiveOrderBySortOrderAsc(Boolean isActive);
}
