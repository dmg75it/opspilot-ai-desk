package com.opspilot.desk.dto.dashboard;

import com.opspilot.desk.dto.ticket.TicketResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    private Map<String, Long> ticketsByStatus;
    private Map<String, Long> ticketsByPriority;
    private List<TicketResponse> myOpenTickets;
    private List<TicketResponse> recentTickets;
    private long aiInteractionsToday;
}
