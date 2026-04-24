package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.Goal;
import com.iterate.digitalwellness.repository.GoalRepository;
import com.iterate.digitalwellness.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalServiceImpl implements GoalService {
    @Autowired
    private GoalRepository goalRepository;

    @Override
    public Goal save(Goal goal) {
        return goalRepository.save(goal);
    }

    @Override
    public List<Goal> findAll() {
        return goalRepository.findAll();
    }

    @Override
    public Goal findById(Long id) {
        return goalRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        goalRepository.deleteById(id);
    }
}