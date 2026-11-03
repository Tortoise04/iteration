package com.smartschedule.dto;

import lombok.Data;

/**
 * PUT /api/taskbooks/{id} 请求体，三个字段都可省。
 */
@Data
public class TaskBookUpdateRequest {
    private String title;
    private String purpose;
    private String requirements;
}
