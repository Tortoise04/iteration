package com.smartschedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AiService.generateTaskBookJson 的解析目标（契约 §2）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskBookResponse {

    private String title;
    private String purpose;
    private String requirements;
    private List<AiWeek> weeks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiWeek {
        private Integer weekIndex;
        private String theme;
        private String focus;
        private String startDate;
        private String endDate;
        private List<AiItem> items;

        @JsonProperty("week_index")
        void setWeekIndexSnake(Integer v) { if (weekIndex == null) weekIndex = v; }

        @JsonProperty("start_date")
        void setStartDateSnake(String v) { if (startDate == null) startDate = v; }

        @JsonProperty("end_date")
        void setEndDateSnake(String v) { if (endDate == null) endDate = v; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiItem {
        private String planDate;
        private String title;
        private String learnContent;
        private Integer estMinutes;
        private String doneCriteria;
        private String category;

        @JsonProperty("plan_date")
        void setPlanDateSnake(String v) { if (planDate == null) planDate = v; }

        @JsonProperty("learn_content")
        void setLearnContentSnake(String v) { if (learnContent == null) learnContent = v; }

        @JsonProperty("est_minutes")
        void setEstMinutesSnake(Integer v) { if (estMinutes == null) estMinutes = v; }

        @JsonProperty("done_criteria")
        void setDoneCriteriaSnake(String v) { if (doneCriteria == null) doneCriteria = v; }
    }
}
