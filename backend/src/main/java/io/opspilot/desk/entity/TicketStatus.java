package io.opspilot.desk.entity;

import java.util.Set;

public enum TicketStatus {
    NEW, IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED, CLOSED;

    private static final java.util.Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = java.util.Map.of(
            NEW, Set.of(IN_PROGRESS, CLOSED),
            IN_PROGRESS, Set.of(WAITING_FOR_CUSTOMER, RESOLVED, CLOSED),
            WAITING_FOR_CUSTOMER, Set.of(IN_PROGRESS, RESOLVED, CLOSED),
            RESOLVED, Set.of(IN_PROGRESS, CLOSED),
            CLOSED, Set.of()
    );

    public boolean canTransitionTo(TicketStatus target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
