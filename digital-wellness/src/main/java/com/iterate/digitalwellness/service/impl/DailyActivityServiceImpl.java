package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.DailyActivity;
import com.iterate.digitalwellness.repository.DailyActivityRepository;
import com.iterate.digitalwellness.service.DailyActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyActivityServiceImpl implements DailyActivityService {
    @Autowired
    private DailyActivityRepository dailyActivityRepository;

    @Override
    public DailyActivity save(DailyActivity dailyActivity) {
        return dailyActivityRepository.save(dailyActivity);
    }

    @Override
    public List<DailyActivity> findAll() {
        return dailyActivityRepository.findAll();
    }

    @Override
    public DailyActivity findById(Long id) {
        return dailyActivityRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        dailyActivityRepository.deleteById(id);
    }

    @Override
    public List<DailyActivity> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return dailyActivityRepository.findByDateBetween(startDate, endDate);
    }
}