package io.opspilot.desk.dto.ticket;

import io.opspilot.desk.dto.auth.UserResponse;
import io.opspilot.desk.entity.NoteVisibility;
import io.opspilot.desk.entity.TicketNote;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        UUID ticketId,
        UserResponse author,
        String body,
        NoteVisibility visibility,
        Instant createdAt
) {
    public static NoteResponse from(TicketNote note) {
        return new NoteResponse(
                note.getId(),
                note.getTicket().getId(),
                UserResponse.from(note.getAuthor()),
                note.getBody(),
                note.getVisibility(),
                note.getCreatedAt()
        );
    }
}
