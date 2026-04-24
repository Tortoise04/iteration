package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.Achievement;
import com.iterate.digitalwellness.repository.AchievementRepository;
import com.iterate.digitalwellness.service.AchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AchievementServiceImpl implements AchievementService {
    @Autowired
    private AchievementRepository achievementRepository;

    @Override
    public Achievement save(Achievement achievement) {
        return achievementRepository.save(achievement);
    }

    @Override
    public List<Achievement> findAll() {
        return achievementRepository.findAll();
    }

    @Override
    public Achievement findById(Long id) {
        return achievementRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        achievementRepository.deleteById(id);
    }
}