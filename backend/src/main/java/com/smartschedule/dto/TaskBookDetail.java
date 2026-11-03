package com.smartschedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartschedule.entity.TaskBookItem;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 任务书完整详情（契约 §3 GET /api/taskbooks/{id}）。
 * aiGenerated 只在生成接口返回，其余接口为 null 时不出现在 JSON 中。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskBookDetail {

    private Long id;
    private String title;
    private String purpose;
    private String requirements;
    private Long goalId;
    private String goalName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String status;
    private Progress progress;
    private List<WeekView> weeks;
    private Boolean aiGenerated;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private int done;
        private int total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeekView {
        private Long id;
        private Integer weekIndex;
        private String theme;
        private String focus;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private List<TaskBookItem> items;
    }
}
