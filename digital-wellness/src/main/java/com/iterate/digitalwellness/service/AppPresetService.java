package com.iterate.digitalwellness.service;

import com.iterate.digitalwellness.entity.AppPreset;
import com.iterate.digitalwellness.repository.AppPresetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppPresetService {

    @Autowired
    private AppPresetRepository appPresetRepository;

    public AppPreset save(AppPreset appPreset) {
        return appPresetRepository.save(appPreset);
    }

    public List<AppPreset> findAll() {
        return appPresetRepository.findAll();
    }

    public List<AppPreset> findActive() {
        return appPresetRepository.findByIsActiveOrderBySortOrderAsc(true);
    }

    public AppPreset findById(Long id) {
        return appPresetRepository.findById(id).orElse(null);
    }

    public void deleteById(Long id) {
        appPresetRepository.deleteById(id);
    }
}
