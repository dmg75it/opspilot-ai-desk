package com.opspilot.desk.repository;

import com.opspilot.desk.entity.TicketAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAuditRepository extends JpaRepository<TicketAudit, Long> {
    List<TicketAudit> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
