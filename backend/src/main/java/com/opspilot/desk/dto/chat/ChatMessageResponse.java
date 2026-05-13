package com.opspilot.desk.dto.chat;

import com.opspilot.desk.entity.ChatMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String model;
    private Integer tokenCount;
    private boolean errorFlag;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage m) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.setId(m.getId());
        r.setSessionId(m.getSession().getId());
        r.setRole(m.getRole().name());
        r.setContent(m.getContent());
        r.setModel(m.getModel());
        r.setTokenCount(m.getTokenCount());
        r.setErrorFlag(m.isErrorFlag());
        r.setErrorMessage(m.getErrorMessage());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }
}
