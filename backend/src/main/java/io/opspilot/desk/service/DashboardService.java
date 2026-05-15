package io.opspilot.desk.service;

import io.opspilot.desk.dto.dashboard.DashboardResponse;
import io.opspilot.desk.entity.Ticket;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final TicketService ticketService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String userEmail) {
        var byStatus = Arrays.stream(Ticket.Status.values())
            .collect(Collectors.toMap(Enum::name, ticketRepository::countByStatus));

        var byPriority = Arrays.stream(Ticket.Priority.values())
            .collect(Collectors.toMap(Enum::name, ticketRepository::countByPriority));

        var myTickets = userRepository.findByEmail(userEmail)
            .map(u -> ticketRepository.findByAssignedToAndStatusNot(u, Ticket.Status.CLOSED)
                .stream().map(ticketService::toResponse).toList())
            .orElse(List.of());

        var recent = ticketRepository.findTop10ByOrderByUpdatedAtDesc()
            .stream().map(ticketService::toResponse).toList();

        long aiToday = aiService.countAiInteractionsToday();

        return new DashboardResponse(byStatus, byPriority, myTickets, recent, aiToday);
    }
}
