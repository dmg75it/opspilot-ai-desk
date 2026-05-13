package com.opspilot.desk.service;

import com.opspilot.desk.entity.Ticket;
import com.opspilot.desk.entity.TicketAudit;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.repository.TicketAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketAuditService {

    private final TicketAuditRepository auditRepository;

    public void record(Ticket ticket, User actor, String action, String oldValue, String newValue) {
        auditRepository.save(TicketAudit.builder()
                .ticket(ticket)
                .actor(actor)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }
}
