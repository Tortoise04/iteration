package com.smartschedule.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class GoalRequest {
    @NotBlank(message = "目标名称不能为空")
    @Size(max = 200, message = "目标名称不能超过200个字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    private String category;

    private String energyLevel;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotNull(message = "默认时长不能为空")
    private Integer defaultDuration;

    private Integer weeklyFrequency;
}
