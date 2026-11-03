package com.smartschedule.dto;

import lombok.Data;

/**
 * POST /api/taskbooks/{bookId}/items 请求体（契约 §3）。
 * planDate/title 必填，其余可省。
 */
@Data
public class TaskBookItemCreateRequest {
    private String planDate;
    private String title;
    private String learnContent;
    private Integer estMinutes;
    private String doneCriteria;
    private String category;
    private Long weekId;
}
