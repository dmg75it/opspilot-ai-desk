package io.opspilot.desk.repository;

import io.opspilot.desk.entity.ChatSession;
import io.opspilot.desk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Optional<ChatSession> findByTicket(Ticket ticket);
}
