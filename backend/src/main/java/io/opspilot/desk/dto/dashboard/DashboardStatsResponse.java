package io.opspilot.desk.dto.dashboard;

import io.opspilot.desk.dto.ticket.TicketResponse;
import io.opspilot.desk.entity.TicketPriority;
import io.opspilot.desk.entity.TicketStatus;

import java.util.List;
import java.util.Map;

public record DashboardStatsResponse(
        Map<TicketStatus, Long> ticketsByStatus,
        Map<TicketPriority, Long> ticketsByPriority,
        List<TicketResponse> myOpenTickets,
        List<TicketResponse> recentlyUpdated,
        long aiInteractionsToday
) {}
