package com.opspilot.desk.dto.ticket;

import com.opspilot.desk.entity.Ticket;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketResponse {

    private Long id;
    private String externalRef;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String category;
    private Long assignedToId;
    private String assignedToName;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Long version;

    public static TicketResponse from(Ticket t) {
        TicketResponse r = new TicketResponse();
        r.setId(t.getId());
        r.setExternalRef(t.getExternalRef());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setStatus(t.getStatus().name());
        r.setPriority(t.getPriority().name());
        r.setCategory(t.getCategory().name());
        if (t.getAssignedTo() != null) {
            r.setAssignedToId(t.getAssignedTo().getId());
            r.setAssignedToName(t.getAssignedTo().getFullName());
        }
        r.setCreatedById(t.getCreatedBy().getId());
        r.setCreatedByName(t.getCreatedBy().getFullName());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        r.setResolvedAt(t.getResolvedAt());
        r.setVersion(t.getVersion());
        return r;
    }
}
