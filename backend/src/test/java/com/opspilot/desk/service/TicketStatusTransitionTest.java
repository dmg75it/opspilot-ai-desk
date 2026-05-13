package com.opspilot.desk.service;

import com.opspilot.desk.entity.enums.TicketStatus;
import com.opspilot.desk.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStatusTransitionTest {

    private TicketStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TicketStatusTransitionValidator();
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, IN_PROGRESS",
            "NEW, CLOSED",
            "IN_PROGRESS, WAITING_FOR_CUSTOMER",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, NEW",
            "WAITING_FOR_CUSTOMER, IN_PROGRESS",
            "WAITING_FOR_CUSTOMER, CLOSED",
            "RESOLVED, CLOSED",
            "RESOLVED, IN_PROGRESS"
    })
    void validate_allowedTransition_doesNotThrow(String from, String to) {
        assertThatNoException().isThrownBy(() ->
                validator.validate(TicketStatus.valueOf(from), TicketStatus.valueOf(to)));
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, RESOLVED",
            "NEW, WAITING_FOR_CUSTOMER",
            "IN_PROGRESS, CLOSED",
            "WAITING_FOR_CUSTOMER, NEW",
            "WAITING_FOR_CUSTOMER, RESOLVED",
            "RESOLVED, NEW",
            "RESOLVED, WAITING_FOR_CUSTOMER",
            "CLOSED, NEW",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED"
    })
    void validate_invalidTransition_throwsException(String from, String to) {
        assertThatThrownBy(() ->
                validator.validate(TicketStatus.valueOf(from), TicketStatus.valueOf(to)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void isAllowed_validTransition_returnsTrue() {
        assertThat(validator.isAllowed(TicketStatus.NEW, TicketStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    void isAllowed_invalidTransition_returnsFalse() {
        assertThat(validator.isAllowed(TicketStatus.CLOSED, TicketStatus.NEW)).isFalse();
    }
}
