package com.iterate.digitalwellness.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PhoneUsageCreateDTO {
    private Long id;
    private LocalDate date;
    private Long usageTime;
    private List<AppUsageDetailDTO> appDetails = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getUsageTime() {
        return usageTime;
    }

    public void setUsageTime(Long usageTime) {
        this.usageTime = usageTime;
    }

    public List<AppUsageDetailDTO> getAppDetails() {
        return appDetails;
    }

    public void setAppDetails(List<AppUsageDetailDTO> appDetails) {
        this.appDetails = appDetails;
    }
}
