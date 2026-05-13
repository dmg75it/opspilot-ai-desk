package com.opspilot.desk.service;

import com.opspilot.desk.dto.dashboard.DashboardResponse;
import com.opspilot.desk.dto.ticket.TicketResponse;
import com.opspilot.desk.entity.Ticket;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.entity.enums.TicketPriority;
import com.opspilot.desk.entity.enums.TicketStatus;
import com.opspilot.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(User currentUser) {
        List<Ticket> allTickets = ticketRepository.findAll();

        Map<String, Long> byStatus = Arrays.stream(TicketStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        s -> allTickets.stream().filter(t -> t.getStatus() == s).count()
                ));

        Map<String, Long> byPriority = Arrays.stream(TicketPriority.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        p -> allTickets.stream().filter(t -> t.getPriority() == p).count()
                ));

        List<TicketResponse> myOpenTickets = ticketRepository
                .findOpenTicketsAssignedTo(currentUser.getId())
                .stream()
                .map(TicketResponse::from)
                .toList();

        List<TicketResponse> recentTickets = ticketRepository
                .findTop10ByOrderByUpdatedAtDesc()
                .stream()
                .map(TicketResponse::from)
                .toList();

        long aiInteractionsToday = ticketRepository.countAiInteractionsToday();

        return DashboardResponse.builder()
                .ticketsByStatus(byStatus)
                .ticketsByPriority(byPriority)
                .myOpenTickets(myOpenTickets)
                .recentTickets(recentTickets)
                .aiInteractionsToday(aiInteractionsToday)
                .build();
    }
}
