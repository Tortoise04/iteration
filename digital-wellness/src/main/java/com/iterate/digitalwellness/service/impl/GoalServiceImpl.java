package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.Goal;
import com.iterate.digitalwellness.repository.GoalRepository;
import com.iterate.digitalwellness.service.GoalService;
import com.iterate.digitalwellness.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalServiceImpl implements GoalService {
    @Autowired
    private GoalRepository goalRepository;

    @Override
    public Goal save(Goal goal) {
        // 设置当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && goal.getUserId() == null) {
            goal.setUserId(userId);
        }
        return goalRepository.save(goal);
    }

    @Override
    public List<Goal> findAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return goalRepository.findByUserId(userId);
        }
        return goalRepository.findAll();
    }

    @Override
    public Goal findById(Long id) {
        Goal goal = goalRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (goal != null && userId != null && goal.getUserId() != null && !goal.getUserId().equals(userId)) {
            return null;
        }
        return goal;
    }

    @Override
    public void deleteById(Long id) {
        Goal goal = goalRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (goal != null && userId != null && goal.getUserId() != null && !goal.getUserId().equals(userId)) {
            return;
        }
        goalRepository.deleteById(id);
    }

    @Override
    public List<Goal> findByUserId(Long userId) {
        return goalRepository.findByUserId(userId);
    }
}