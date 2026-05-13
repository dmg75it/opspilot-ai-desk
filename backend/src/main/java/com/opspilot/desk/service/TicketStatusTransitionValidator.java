package com.opspilot.desk.service;

import com.opspilot.desk.entity.enums.TicketStatus;
import com.opspilot.desk.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class TicketStatusTransitionValidator {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TicketStatus.NEW,
                EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(TicketStatus.IN_PROGRESS,
                EnumSet.of(TicketStatus.WAITING_FOR_CUSTOMER, TicketStatus.RESOLVED, TicketStatus.NEW));
        ALLOWED_TRANSITIONS.put(TicketStatus.WAITING_FOR_CUSTOMER,
                EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(TicketStatus.RESOLVED,
                EnumSet.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(TicketStatus.CLOSED,
                EnumSet.noneOf(TicketStatus.class));
    }

    public void validate(TicketStatus from, TicketStatus to) {
        Set<TicketStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TicketStatus.class));
        if (!allowed.contains(to)) {
            throw new InvalidStatusTransitionException(from.name(), to.name());
        }
    }

    public boolean isAllowed(TicketStatus from, TicketStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TicketStatus.class)).contains(to);
    }
}
