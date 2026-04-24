package com.iterate.digitalwellness.service;

import com.iterate.digitalwellness.entity.Goal;

import java.util.List;

public interface GoalService {
    Goal save(Goal goal);
    List<Goal> findAll();
    Goal findById(Long id);
    void deleteById(Long id);
}