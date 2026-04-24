package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.DailyActivity;
import com.iterate.digitalwellness.service.DailyActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-activities")
public class DailyActivityController {
    @Autowired
    private DailyActivityService dailyActivityService;

    @PostMapping
    public DailyActivity save(@RequestBody DailyActivity dailyActivity) {
        return dailyActivityService.save(dailyActivity);
    }

    @GetMapping
    public List<DailyActivity> findAll() {
        return dailyActivityService.findAll();
    }

    @GetMapping("/{id}")
    public DailyActivity findById(@PathVariable Long id) {
        return dailyActivityService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        dailyActivityService.deleteById(id);
    }

    @GetMapping("/date-range")
    public List<DailyActivity> findByDateBetween(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return dailyActivityService.findByDateBetween(startDate, endDate);
    }
}