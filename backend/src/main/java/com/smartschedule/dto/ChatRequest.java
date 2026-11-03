package com.smartschedule.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息不能超过2000个字符")
    private String message;

    private String sessionId;
}
