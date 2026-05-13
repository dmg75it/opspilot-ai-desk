package com.opspilot.desk.dto.ticket;

import com.opspilot.desk.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeStatusRequest {

    @NotNull
    private TicketStatus status;
}
