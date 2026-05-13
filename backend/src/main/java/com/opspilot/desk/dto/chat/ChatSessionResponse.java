package com.opspilot.desk.dto.chat;

import com.opspilot.desk.entity.ChatSession;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionResponse {

    private Long id;
    private Long ticketId;
    private Long createdById;
    private LocalDateTime createdAt;

    public static ChatSessionResponse from(ChatSession s) {
        ChatSessionResponse r = new ChatSessionResponse();
        r.setId(s.getId());
        r.setTicketId(s.getTicket().getId());
        r.setCreatedById(s.getCreatedBy().getId());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }
}
