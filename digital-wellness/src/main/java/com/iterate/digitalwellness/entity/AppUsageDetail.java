package com.iterate.digitalwellness.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AppUsageDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_usage_id")
    private PhoneUsage phoneUsage;
    
    private String appName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_preset_id")
    private AppPreset appPreset;
    
    private Long usageTime;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PhoneUsage getPhoneUsage() {
        return phoneUsage;
    }

    public void setPhoneUsage(PhoneUsage phoneUsage) {
        this.phoneUsage = phoneUsage;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public AppPreset getAppPreset() {
        return appPreset;
    }

    public void setAppPreset(AppPreset appPreset) {
        this.appPreset = appPreset;
    }

    public Long getUsageTime() {
        return usageTime;
    }

    public void setUsageTime(Long usageTime) {
        this.usageTime = usageTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
