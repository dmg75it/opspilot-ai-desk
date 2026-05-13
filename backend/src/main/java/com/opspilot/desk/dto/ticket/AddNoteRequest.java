package com.opspilot.desk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddNoteRequest {

    @NotBlank
    private String body;
}
