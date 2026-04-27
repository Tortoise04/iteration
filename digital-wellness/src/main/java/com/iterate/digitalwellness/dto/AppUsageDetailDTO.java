package com.iterate.digitalwellness.dto;

public class AppUsageDetailDTO {
    private Long id;
    private Long phoneUsageId;
    private String appName;
    private Long appPresetId;
    private Long usageTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPhoneUsageId() {
        return phoneUsageId;
    }

    public void setPhoneUsageId(Long phoneUsageId) {
        this.phoneUsageId = phoneUsageId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Long getAppPresetId() {
        return appPresetId;
    }

    public void setAppPresetId(Long appPresetId) {
        this.appPresetId = appPresetId;
    }

    public Long getUsageTime() {
        return usageTime;
    }

    public void setUsageTime(Long usageTime) {
        this.usageTime = usageTime;
    }
}
