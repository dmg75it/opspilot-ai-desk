package io.opspilot.desk.ticket;

import io.opspilot.desk.entity.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTransitionTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "NEW,IN_PROGRESS,true",
            "NEW,CLOSED,true",
            "NEW,RESOLVED,false",
            "IN_PROGRESS,WAITING_FOR_CUSTOMER,true",
            "IN_PROGRESS,RESOLVED,true",
            "IN_PROGRESS,CLOSED,true",
            "IN_PROGRESS,NEW,false",
            "WAITING_FOR_CUSTOMER,IN_PROGRESS,true",
            "WAITING_FOR_CUSTOMER,RESOLVED,true",
            "WAITING_FOR_CUSTOMER,CLOSED,true",
            "WAITING_FOR_CUSTOMER,NEW,false",
            "RESOLVED,IN_PROGRESS,true",
            "RESOLVED,CLOSED,true",
            "RESOLVED,NEW,false",
            "CLOSED,IN_PROGRESS,false",
            "CLOSED,RESOLVED,false",
    })
    void transitionValidation(String from, String to, boolean expected) {
        TicketStatus fromStatus = TicketStatus.valueOf(from);
        TicketStatus toStatus = TicketStatus.valueOf(to);
        assertThat(fromStatus.canTransitionTo(toStatus)).isEqualTo(expected);
    }

    @Test
    void closedStatusHasNoValidTransitions() {
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(TicketStatus.CLOSED.canTransitionTo(target)).isFalse();
        }
    }
}
