package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.Achievement;
import com.iterate.digitalwellness.service.AchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {
    @Autowired
    private AchievementService achievementService;

    @PostMapping
    public Achievement save(@RequestBody Achievement achievement) {
        return achievementService.save(achievement);
    }

    @GetMapping
    public List<Achievement> findAll() {
        return achievementService.findAll();
    }

    @GetMapping("/{id}")
    public Achievement findById(@PathVariable Long id) {
        return achievementService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        achievementService.deleteById(id);
    }
}