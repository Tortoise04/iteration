package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.repository.PhoneUsageRepository;
import com.iterate.digitalwellness.service.PhoneUsageService;
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
        return phoneUsageRepository.save(phoneUsage);
    }

    @Override
    public List<PhoneUsage> findAll() {
        return phoneUsageRepository.findAll();
    }

    @Override
    public List<PhoneUsage> findByUserId(Long userId) {
        return phoneUsageRepository.findByUserId(userId);
    }

    @Override
    public PhoneUsage findById(Long id) {
        return phoneUsageRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        phoneUsageRepository.deleteById(id);
    }

    @Override
    public List<PhoneUsage> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return phoneUsageRepository.findByDateBetween(startDate, endDate);
    }

    @Override
    public List<PhoneUsage> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        return phoneUsageRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }
}
