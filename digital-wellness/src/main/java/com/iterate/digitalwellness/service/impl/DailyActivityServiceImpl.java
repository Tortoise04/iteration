package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.DailyActivity;
import com.iterate.digitalwellness.repository.DailyActivityRepository;
import com.iterate.digitalwellness.service.DailyActivityService;
import com.iterate.digitalwellness.util.SecurityUtils;
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
        // 设置当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && dailyActivity.getUserId() == null) {
            dailyActivity.setUserId(userId);
        }
        return dailyActivityRepository.save(dailyActivity);
    }

    @Override
    public List<DailyActivity> findAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return dailyActivityRepository.findByUserId(userId);
        }
        return dailyActivityRepository.findAll();
    }

    @Override
    public List<DailyActivity> findByUserId(Long userId) {
        return dailyActivityRepository.findByUserId(userId);
    }

    @Override
    public DailyActivity findById(Long id) {
        DailyActivity activity = dailyActivityRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (activity != null && userId != null && activity.getUserId() != null && !activity.getUserId().equals(userId)) {
            return null;
        }
        return activity;
    }

    @Override
    public void deleteById(Long id) {
        DailyActivity activity = dailyActivityRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (activity != null && userId != null && activity.getUserId() != null && !activity.getUserId().equals(userId)) {
            return;
        }
        dailyActivityRepository.deleteById(id);
    }

    @Override
    public List<DailyActivity> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return dailyActivityRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        }
        return dailyActivityRepository.findByDateBetween(startDate, endDate);
    }

    @Override
    public List<DailyActivity> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        return dailyActivityRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }
}