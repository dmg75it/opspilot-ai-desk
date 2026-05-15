package io.opspilot.desk.dto.dashboard;

import io.opspilot.desk.dto.ticket.TicketResponse;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
    Map<String, Long> ticketsByStatus,
    Map<String, Long> ticketsByPriority,
    List<TicketResponse> myOpenTickets,
    List<TicketResponse> recentlyUpdated,
    long aiInteractionsToday) {}
