package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.repository.PhoneUsageRepository;
import com.iterate.digitalwellness.service.PhoneUsageService;
import com.iterate.digitalwellness.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PhoneUsageServiceImpl implements PhoneUsageService {
    @Autowired
    private PhoneUsageRepository phoneUsageRepository;

    @Override
    public PhoneUsage save(PhoneUsage phoneUsage) {
        // 设置当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && phoneUsage.getUserId() == null) {
            phoneUsage.setUserId(userId);
        }
        
        // 查重逻辑：根据用户ID和日期查询是否已存在
        if (phoneUsage.getUserId() != null && phoneUsage.getDate() != null) {
            PhoneUsage existing = phoneUsageRepository.findByUserIdAndDate(phoneUsage.getUserId(), phoneUsage.getDate());
            if (existing != null) {
                return existing;
            }
        }
        
        return phoneUsageRepository.save(phoneUsage);
    }

    @Override
    public List<PhoneUsage> findAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return phoneUsageRepository.findByUserId(userId);
        }
        return phoneUsageRepository.findAll();
    }

    @Override
    public List<PhoneUsage> findByUserId(Long userId) {
        return phoneUsageRepository.findByUserId(userId);
    }

    @Override
    public PhoneUsage findById(Long id) {
        PhoneUsage phoneUsage = phoneUsageRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (phoneUsage != null && userId != null && phoneUsage.getUserId() != null && !phoneUsage.getUserId().equals(userId)) {
            return null;
        }
        return phoneUsage;
    }

    @Override
    public void deleteById(Long id) {
        PhoneUsage phoneUsage = phoneUsageRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (phoneUsage != null && userId != null && phoneUsage.getUserId() != null && !phoneUsage.getUserId().equals(userId)) {
            return;
        }
        phoneUsageRepository.deleteById(id);
    }

    @Override
    public List<PhoneUsage> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return phoneUsageRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        }
        return phoneUsageRepository.findByDateBetween(startDate, endDate);
    }

    @Override
    public List<PhoneUsage> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        return phoneUsageRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }
}
