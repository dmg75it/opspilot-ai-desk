package com.opspilot.desk.dto.ticket;

import com.opspilot.desk.entity.enums.TicketCategory;
import com.opspilot.desk.entity.enums.TicketPriority;
import com.opspilot.desk.entity.enums.TicketStatus;
import lombok.Data;

@Data
public class TicketFilterParams {
    private TicketStatus status;
    private TicketPriority priority;
    private TicketCategory category;
    private Long assignedToId;
    private int page = 0;
    private int size = 20;
}
