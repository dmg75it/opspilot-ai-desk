package com.opspilot.desk.dto.ticket;

import com.opspilot.desk.entity.enums.TicketCategory;
import com.opspilot.desk.entity.enums.TicketPriority;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTicketRequest {

    private String externalRef;

    @Size(max = 150)
    private String title;

    @Size(max = 5000)
    private String description;

    private TicketPriority priority;

    private TicketCategory category;
}
