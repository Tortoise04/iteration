package com.smartschedule.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseRequest {
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 200, message = "课程名称不能超过200个字符")
    private String name;

    @NotNull(message = "星期不能为空")
    private Integer dayOfWeek;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @Size(max = 200, message = "地点不能超过200个字符")
    private String location;
}
