package com.smartschedule.dto;

import lombok.Data;

/**
 * PUT /api/taskbooks/items/{itemId} 请求体，字段都可省。
 */
@Data
public class TaskBookItemUpdateRequest {
    private String title;
    private String learnContent;
    private Integer estMinutes;
    private String doneCriteria;
    private String planDate;
}
