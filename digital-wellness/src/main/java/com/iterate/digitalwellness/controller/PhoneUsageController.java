package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.service.PhoneUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/phone-usage")
public class PhoneUsageController {
    @Autowired
    private PhoneUsageService phoneUsageService;

    @PostMapping
    public PhoneUsage save(@RequestBody PhoneUsage phoneUsage) {
        return phoneUsageService.save(phoneUsage);
    }

    @GetMapping
    public List<PhoneUsage> findAll() {
        return phoneUsageService.findAll();
    }

    @GetMapping("/{id}")
    public PhoneUsage findById(@PathVariable Long id) {
        return phoneUsageService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        phoneUsageService.deleteById(id);
    }

    @GetMapping("/date-range")
    public List<PhoneUsage> findByDateBetween(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return phoneUsageService.findByDateBetween(startDate, endDate);
    }
}