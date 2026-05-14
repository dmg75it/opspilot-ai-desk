package com.opspilot.desk.repository;

import com.opspilot.desk.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Optional<ChatSession> findByTicketIdAndUserId(UUID ticketId, UUID userId);
}
