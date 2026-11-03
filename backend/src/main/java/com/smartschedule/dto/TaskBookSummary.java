package com.smartschedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;

/**
 * 任务书列表行（契约 §3 GET /api/taskbooks）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskBookSummary {
    private Long id;
    private String title;
    private Long goalId;
    private String goalName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String status;
    private Integer totalItems;
    private Integer doneItems;
}
