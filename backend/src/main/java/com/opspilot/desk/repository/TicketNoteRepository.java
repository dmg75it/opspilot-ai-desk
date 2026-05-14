package com.opspilot.desk.repository;

import com.opspilot.desk.entity.TicketNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketNoteRepository extends JpaRepository<TicketNote, UUID> {
    List<TicketNote> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
