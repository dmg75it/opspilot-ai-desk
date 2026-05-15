package io.opspilot.desk.service;

import io.opspilot.desk.dto.ticket.*;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.entity.Ticket.*;
import io.opspilot.desk.exception.*;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        Status.NEW,                  Set.of(Status.IN_PROGRESS, Status.CLOSED),
        Status.IN_PROGRESS,          Set.of(Status.WAITING_FOR_CUSTOMER, Status.RESOLVED, Status.CLOSED),
        Status.WAITING_FOR_CUSTOMER, Set.of(Status.IN_PROGRESS, Status.RESOLVED, Status.CLOSED),
        Status.RESOLVED,             Set.of(Status.CLOSED, Status.IN_PROGRESS),
        Status.CLOSED,               Set.of()
    );

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NoteService noteService;

    public void validateTransition(Status from, Status to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest req, String creatorEmail) {
        var creator = userRepository.findByEmail(creatorEmail).orElseThrow();
        var ticket = new Ticket();
        ticket.setTitle(req.title());
        ticket.setDescription(req.description());
        ticket.setPriority(Priority.valueOf(req.priority()));
        ticket.setCategory(Category.valueOf(req.category()));
        ticket.setExternalRef(req.externalRef());
        ticket.setCreatedBy(creator);
        var saved = ticketRepository.save(ticket);
        log.info("Ticket created id={} by={}", saved.getId(), creatorEmail);
        auditService.log(saved, creator, "CREATED", null, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(UUID id) {
        return toResponse(findTicket(id));
    }

    @Transactional
    public TicketResponse update(UUID id, UpdateTicketRequest req, String userEmail) {
        var ticket = findTicket(id);
        var user = userRepository.findByEmail(userEmail).orElseThrow();
        if (ticket.getStatus() == Status.CLOSED && !isAdmin(user)) {
            throw new AccessDeniedException("Only ADMIN can edit closed tickets");
        }
        if (req.title() != null) ticket.setTitle(req.title());
        if (req.description() != null) ticket.setDescription(req.description());
        if (req.priority() != null) ticket.setPriority(Priority.valueOf(req.priority()));
        if (req.category() != null) ticket.setCategory(Category.valueOf(req.category()));
        if (req.externalRef() != null) ticket.setExternalRef(req.externalRef());
        log.info("Ticket updated id={} by={}", id, userEmail);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse changeStatus(UUID id, ChangeStatusRequest req, String userEmail) {
        var ticket = findTicket(id);
        var user = userRepository.findByEmail(userEmail).orElseThrow();
        var newStatus = Status.valueOf(req.status());
        validateTransition(ticket.getStatus(), newStatus);
        var old = ticket.getStatus();
        ticket.setStatus(newStatus);
        if (newStatus == Status.RESOLVED) ticket.setResolvedAt(Instant.now());
        ticketRepository.save(ticket);
        log.info("Ticket status changed id={} {}→{} by={}", id, old, newStatus, userEmail);
        auditService.log(ticket, user, "STATUS_CHANGED", old.name(), newStatus.name());
        noteService.addSystemNote(ticket, "Status changed: " + old + " -> " + newStatus);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse assign(UUID id, AssignTicketRequest req, String userEmail) {
        var ticket = findTicket(id);
        var actor = userRepository.findByEmail(userEmail).orElseThrow();
        var assignee = req.assigneeId() != null
            ? userRepository.findById(req.assigneeId()).orElseThrow()
            : null;
        var old = ticket.getAssignedTo();
        ticket.setAssignedTo(assignee);
        ticketRepository.save(ticket);
        auditService.log(ticket, actor, "ASSIGNED",
            old != null ? old.getEmail() : null,
            assignee != null ? assignee.getEmail() : null);
        return toResponse(ticket);
    }

    private Ticket findTicket(UUID id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }

    public TicketResponse toResponse(Ticket t) {
        return new TicketResponse(
            t.getId(), t.getExternalRef(), t.getTitle(), t.getDescription(),
            t.getStatus().name(), t.getPriority().name(), t.getCategory().name(),
            t.getAssignedTo() != null ? t.getAssignedTo().getEmail() : null,
            t.getCreatedBy().getEmail(), t.getCreatedAt(), t.getUpdatedAt(),
            t.getResolvedAt(), t.getVersion()
        );
    }
}
