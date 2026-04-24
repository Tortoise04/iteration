package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.PeriodSummary;
import com.iterate.digitalwellness.service.PeriodSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/period-summaries")
public class PeriodSummaryController {
    @Autowired
    private PeriodSummaryService periodSummaryService;

    @PostMapping
    public PeriodSummary save(@RequestBody PeriodSummary periodSummary) {
        return periodSummaryService.save(periodSummary);
    }

    @GetMapping
    public List<PeriodSummary> findAll() {
        return periodSummaryService.findAll();
    }

    @GetMapping("/{id}")
    public PeriodSummary findById(@PathVariable Long id) {
        return periodSummaryService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        periodSummaryService.deleteById(id);
    }

    @PostMapping("/generate")
    public PeriodSummary generateSummary(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate, @RequestParam String periodType) {
        return periodSummaryService.generateSummary(startDate, endDate, periodType);
    }
}