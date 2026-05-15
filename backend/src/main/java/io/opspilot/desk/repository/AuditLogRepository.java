package io.opspilot.desk.repository;

import io.opspilot.desk.entity.AuditLog;
import io.opspilot.desk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTicketOrderByCreatedAtDesc(Ticket ticket);
}
