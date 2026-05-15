package io.opspilot.desk.exception;

import io.opspilot.desk.entity.Ticket;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Ticket.Status from, Ticket.Status to) {
        super("Cannot transition ticket from " + from + " to " + to);
    }
}
