package com.smartschedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiScheduleResponse {

    private List<AiScheduleTask> tasks;
    private String suggestion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiScheduleTask {
        private String name;
        private String taskType;
        private String startTime;
        private String endTime;
        private Integer durationMinutes;
        private String energyLevel;
        private Integer priority;
        private String goalName;
        private String courseName;

        // Accept both camelCase and snake_case from AI
        @JsonProperty("goal_name")
        void setGoalNameSnake(String v) { if (goalName == null) goalName = v; }
        @JsonProperty("course_name")
        void setCourseNameSnake(String v) { if (courseName == null) courseName = v; }
        @JsonProperty("task_type")
        void setTaskTypeSnake(String v) { if (taskType == null) taskType = v; }
        @JsonProperty("start_time")
        void setStartTimeSnake(String v) { if (startTime == null) startTime = v; }
        @JsonProperty("end_time")
        void setEndTimeSnake(String v) { if (endTime == null) endTime = v; }
        @JsonProperty("duration_minutes")
        void setDurationMinutesSnake(Integer v) { if (durationMinutes == null) durationMinutes = v; }
        @JsonProperty("energy_level")
        void setEnergyLevelSnake(String v) { if (energyLevel == null) energyLevel = v; }
    }
}
