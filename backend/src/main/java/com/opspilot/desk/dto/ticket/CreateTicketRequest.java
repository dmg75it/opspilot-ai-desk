package com.opspilot.desk.dto.ticket;

import com.opspilot.desk.entity.enums.TicketCategory;
import com.opspilot.desk.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTicketRequest {

    private String externalRef;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 5000)
    private String description;

    private TicketPriority priority = TicketPriority.MEDIUM;

    private TicketCategory category = TicketCategory.OTHER;

    private Long assignedToId;
}
