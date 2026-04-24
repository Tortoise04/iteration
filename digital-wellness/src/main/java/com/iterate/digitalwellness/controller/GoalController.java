package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.Goal;
import com.iterate.digitalwellness.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
    @Autowired
    private GoalService goalService;

    @PostMapping
    public Goal save(@RequestBody Goal goal) {
        return goalService.save(goal);
    }

    @GetMapping
    public List<Goal> findAll() {
        return goalService.findAll();
    }

    @GetMapping("/{id}")
    public Goal findById(@PathVariable Long id) {
        return goalService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        goalService.deleteById(id);
    }
}