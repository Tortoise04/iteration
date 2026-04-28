package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.Achievement;
import com.iterate.digitalwellness.repository.AchievementRepository;
import com.iterate.digitalwellness.service.AchievementService;
import com.iterate.digitalwellness.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AchievementServiceImpl implements AchievementService {
    @Autowired
    private AchievementRepository achievementRepository;

    @Override
    public Achievement save(Achievement achievement) {
        // 设置当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && achievement.getUserId() == null) {
            achievement.setUserId(userId);
        }
        return achievementRepository.save(achievement);
    }

    @Override
    public List<Achievement> findAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return achievementRepository.findByUserId(userId);
        }
        return achievementRepository.findAll();
    }

    @Override
    public List<Achievement> findByUserId(Long userId) {
        return achievementRepository.findByUserId(userId);
    }

    @Override
    public Achievement findById(Long id) {
        Achievement achievement = achievementRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (achievement != null && userId != null && achievement.getUserId() != null && !achievement.getUserId().equals(userId)) {
            return null;
        }
        return achievement;
    }

    @Override
    public void deleteById(Long id) {
        Achievement achievement = achievementRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (achievement != null && userId != null && achievement.getUserId() != null && !achievement.getUserId().equals(userId)) {
            return;
        }
        achievementRepository.deleteById(id);
    }
}