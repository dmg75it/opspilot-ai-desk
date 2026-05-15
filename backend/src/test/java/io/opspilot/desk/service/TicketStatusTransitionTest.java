package io.opspilot.desk.service;

import io.opspilot.desk.entity.Ticket.Status;
import io.opspilot.desk.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TicketStatusTransitionTest {
    private final TicketService service = new TicketService(null, null, null, null);

    @Test
    void newToInProgress_allowed() {
        assertThatCode(() -> service.validateTransition(Status.NEW, Status.IN_PROGRESS))
            .doesNotThrowAnyException();
    }

    @Test
    void newToResolved_notAllowed() {
        assertThatThrownBy(() -> service.validateTransition(Status.NEW, Status.RESOLVED))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void closedToAny_notAllowed() {
        assertThatThrownBy(() -> service.validateTransition(Status.CLOSED, Status.IN_PROGRESS))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void resolvedToClosed_allowed() {
        assertThatCode(() -> service.validateTransition(Status.RESOLVED, Status.CLOSED))
            .doesNotThrowAnyException();
    }

    @Test
    void inProgressToWaiting_allowed() {
        assertThatCode(() -> service.validateTransition(Status.IN_PROGRESS, Status.WAITING_FOR_CUSTOMER))
            .doesNotThrowAnyException();
    }
}
