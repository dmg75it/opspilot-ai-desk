package com.opspilot.desk.repository;

import com.opspilot.desk.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByTicketId(Long ticketId);
}
