package com.opspilot.desk;

import com.opspilot.desk.dto.*;
import com.opspilot.desk.entity.*;
import com.opspilot.desk.repository.*;
import com.opspilot.desk.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketStatusTransitionTest {

    @Mock TicketRepository ticketRepository;
    @Mock UserRepository userRepository;
    @Mock TicketNoteRepository noteRepository;
    @Mock TicketAuditRepository auditRepository;
    @InjectMocks TicketService ticketService;

    @Test
    void validTransition_newToInProgress() {
        Ticket ticket = buildTicket(TicketStatus.NEW);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser user = buildUser(UserRole.OPERATOR);
        TicketResponse result = ticketService.changeStatus(ticket.getId(), new TicketStatusRequest("IN_PROGRESS"), user);
        assertThat(result.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void invalidTransition_closedToAny_throws() {
        Ticket ticket = buildTicket(TicketStatus.CLOSED);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));

        AppUser user = buildUser(UserRole.OPERATOR);
        assertThatThrownBy(() -> ticketService.changeStatus(ticket.getId(), new TicketStatusRequest("IN_PROGRESS"), user))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void invalidTransition_newToResolved_throws() {
        Ticket ticket = buildTicket(TicketStatus.NEW);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));

        AppUser user = buildUser(UserRole.OPERATOR);
        assertThatThrownBy(() -> ticketService.changeStatus(ticket.getId(), new TicketStatusRequest("RESOLVED"), user))
            .isInstanceOf(ResponseStatusException.class);
    }

    private Ticket buildTicket(TicketStatus status) {
        AppUser creator = buildUser(UserRole.OPERATOR);
        return Ticket.builder()
            .id(UUID.randomUUID())
            .title("Test ticket")
            .description("Test description")
            .status(status)
            .priority(TicketPriority.MEDIUM)
            .category(TicketCategory.DELIVERY)
            .createdBy(creator)
            .version(0L)
            .build();
    }

    private AppUser buildUser(UserRole role) {
        return AppUser.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .passwordHash("hash")
            .fullName("Test User")
            .role(role)
            .enabled(true)
            .build();
    }
}
