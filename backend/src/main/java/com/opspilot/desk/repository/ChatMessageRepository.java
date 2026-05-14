package com.opspilot.desk.repository;

import com.opspilot.desk.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.createdAt >= :since AND m.role = 'USER'")
    long countUserMessagesSince(Instant since);
}
