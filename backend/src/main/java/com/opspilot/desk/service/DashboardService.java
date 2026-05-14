package com.opspilot.desk.service;

import com.opspilot.desk.dto.DashboardResponse;
import com.opspilot.desk.dto.TicketSummaryResponse;
import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.repository.ChatMessageRepository;
import com.opspilot.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TicketService ticketService;

    public DashboardResponse getDashboard(AppUser currentUser) {
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : ticketRepository.countByStatus()) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }
        Map<String, Long> byPriority = new HashMap<>();
        for (Object[] row : ticketRepository.countByPriority()) {
            byPriority.put(row[0].toString(), (Long) row[1]);
        }

        List<TicketSummaryResponse> myOpenSummary = ticketRepository.findOpenTicketsByAssignee(currentUser)
            .stream().map(ticketService::toSummary).toList();

        List<TicketSummaryResponse> recent = ticketRepository.findTop10ByOrderByUpdatedAtDesc()
            .stream().map(ticketService::toSummary).toList();

        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long aiToday = chatMessageRepository.countUserMessagesSince(startOfDay);

        return new DashboardResponse(byStatus, byPriority, myOpenSummary, recent, aiToday);
    }
}
