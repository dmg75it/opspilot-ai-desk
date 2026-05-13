package com.opspilot.desk.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignTicketRequest {

    @NotNull
    private Long operatorId;
}
