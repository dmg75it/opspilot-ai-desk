package io.opspilot.desk.repository;

import io.opspilot.desk.entity.ChatMessage;
import io.opspilot.desk.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.role = io.opspilot.desk.entity.ChatMessage.Role.ASSISTANT AND m.createdAt >= :since")
    long countAssistantMessagesSince(Instant since);
}
