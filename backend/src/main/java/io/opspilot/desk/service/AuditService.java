package io.opspilot.desk.service;

import io.opspilot.desk.entity.*;
import io.opspilot.desk.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void log(Ticket ticket, User actor, String action, String oldValue, String newValue) {
        var entry = new AuditLog();
        entry.setTicket(ticket);
        entry.setActor(actor);
        entry.setAction(action);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        auditLogRepository.save(entry);
    }
}
