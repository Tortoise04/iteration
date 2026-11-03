package com.smartschedule.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskStatusRequest {
    @NotBlank(message = "任务状态不能为空")
    private String status;
}
