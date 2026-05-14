package com.opspilot.desk.dto;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
    Map<String, Long> ticketsByStatus,
    Map<String, Long> ticketsByPriority,
    List<TicketSummaryResponse> myOpenTickets,
    List<TicketSummaryResponse> recentlyUpdated,
    long aiInteractionsToday
) {}
