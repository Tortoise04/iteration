package com.smartschedule.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 状态更新请求体：{"status":"..."}。
 */
@Data
public class StatusUpdateRequest {

    @NotBlank(message = "status 不能为空")
    private String status;
}
