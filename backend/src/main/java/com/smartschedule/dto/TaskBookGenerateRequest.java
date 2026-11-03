package com.smartschedule.dto;

import lombok.Data;

/**
 * POST /api/taskbooks/generate 请求体；startDate 可省（默认今天）。
 */
@Data
public class TaskBookGenerateRequest {
    private Long goalId;
    private String startDate;
    private String endDate;
}
