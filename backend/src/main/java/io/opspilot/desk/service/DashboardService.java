package io.opspilot.desk.service;

import io.opspilot.desk.dto.dashboard.DashboardStatsResponse;
import io.opspilot.desk.dto.ticket.TicketResponse;
import io.opspilot.desk.entity.TicketPriority;
import io.opspilot.desk.entity.TicketStatus;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.repository.ChatMessageRepository;
import io.opspilot.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(User user) {
        var byStatus = buildStatusMap();
        var byPriority = buildPriorityMap();
        var myOpen = ticketRepository
                .findByAssignedToAndStatusNot(user, TicketStatus.CLOSED, PageRequest.of(0, 20))
                .getContent().stream().map(TicketResponse::from).toList();
        var recent = ticketRepository.findTop10ByOrderByUpdatedAtDesc().stream()
                .map(TicketResponse::from).toList();
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        long aiToday = chatMessageRepository.countAssistantMessagesSince(todayStart);

        return new DashboardStatsResponse(byStatus, byPriority, myOpen, recent, aiToday);
    }

    private Map<TicketStatus, Long> buildStatusMap() {
        Map<TicketStatus, Long> map = new EnumMap<>(TicketStatus.class);
        for (TicketStatus s : TicketStatus.values()) map.put(s, 0L);
        ticketRepository.countGroupByStatus()
                .forEach(row -> map.put((TicketStatus) row[0], (Long) row[1]));
        return map;
    }

    private Map<TicketPriority, Long> buildPriorityMap() {
        Map<TicketPriority, Long> map = new EnumMap<>(TicketPriority.class);
        for (TicketPriority p : TicketPriority.values()) map.put(p, 0L);
        ticketRepository.countGroupByPriority()
                .forEach(row -> map.put((TicketPriority) row[0], (Long) row[1]));
        return map;
    }
}
