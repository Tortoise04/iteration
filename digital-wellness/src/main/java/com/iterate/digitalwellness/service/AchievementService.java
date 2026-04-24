package com.iterate.digitalwellness.service;

import com.iterate.digitalwellness.entity.Achievement;

import java.util.List;

public interface AchievementService {
    Achievement save(Achievement achievement);
    List<Achievement> findAll();
    Achievement findById(Long id);
    void deleteById(Long id);
}