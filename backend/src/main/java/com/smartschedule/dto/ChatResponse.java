package com.smartschedule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String role;
    private String content;
    private String sessionId;
    private Action action;

    public ChatResponse(String role, String content) {
        this.role = role;
        this.content = content;
        this.action = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private String type;
        private Object data;
    }
}
