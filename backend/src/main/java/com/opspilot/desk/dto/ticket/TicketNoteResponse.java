package com.opspilot.desk.dto.ticket;

import com.opspilot.desk.entity.TicketNote;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketNoteResponse {

    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorName;
    private String body;
    private String visibility;
    private LocalDateTime createdAt;

    public static TicketNoteResponse from(TicketNote n) {
        TicketNoteResponse r = new TicketNoteResponse();
        r.setId(n.getId());
        r.setTicketId(n.getTicket().getId());
        r.setAuthorId(n.getAuthor().getId());
        r.setAuthorName(n.getAuthor().getFullName());
        r.setBody(n.getBody());
        r.setVisibility(n.getVisibility().name());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
