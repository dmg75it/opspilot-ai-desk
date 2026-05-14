package com.opspilot.desk.repository;

import com.opspilot.desk.entity.TicketAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketAuditRepository extends JpaRepository<TicketAudit, UUID> {
    List<TicketAudit> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
