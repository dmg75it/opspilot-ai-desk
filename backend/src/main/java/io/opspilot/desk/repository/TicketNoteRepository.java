package io.opspilot.desk.repository;

import io.opspilot.desk.entity.TicketNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketNoteRepository extends JpaRepository<TicketNote, UUID> {

    List<TicketNote> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
