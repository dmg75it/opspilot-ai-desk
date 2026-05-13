package com.opspilot.desk.service;

import com.opspilot.desk.dto.ticket.*;
import com.opspilot.desk.entity.Ticket;
import com.opspilot.desk.entity.TicketNote;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.entity.enums.NoteVisibility;
import com.opspilot.desk.entity.enums.Role;
import com.opspilot.desk.entity.enums.TicketStatus;
import com.opspilot.desk.exception.TicketNotFoundException;
import com.opspilot.desk.repository.TicketNoteRepository;
import com.opspilot.desk.repository.TicketRepository;
import com.opspilot.desk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final TicketStatusTransitionValidator transitionValidator;
    private final TicketAuditService auditService;

    @Transactional
    public Ticket createTicket(CreateTicketRequest request, User creator) {
        Ticket ticket = Ticket.builder()
                .externalRef(request.getExternalRef())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.NEW)
                .priority(request.getPriority())
                .category(request.getCategory())
                .createdBy(creator)
                .build();

        if (request.getAssignedToId() != null) {
            User operator = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + request.getAssignedToId()));
            ticket.setAssignedTo(operator);
        }

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket created id={} by={}", saved.getId(), creator.getEmail());
        auditService.record(saved, creator, "CREATED", null, saved.getStatus().name());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Ticket> listTickets(TicketFilterParams params) {
        PageRequest pageable = PageRequest.of(params.getPage(), params.getSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ticketRepository.findWithFilters(
                params.getStatus(), params.getPriority(), params.getCategory(),
                params.getAssignedToId(), pageable);
    }

    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id) {
        return ticketRepository.findByIdWithUsers(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public Ticket updateTicket(Long id, UpdateTicketRequest request, User actor) {
        Ticket ticket = getTicketById(id);
        guardClosed(ticket, actor);

        if (request.getTitle() != null) ticket.setTitle(request.getTitle());
        if (request.getDescription() != null) ticket.setDescription(request.getDescription());
        if (request.getPriority() != null) ticket.setPriority(request.getPriority());
        if (request.getCategory() != null) ticket.setCategory(request.getCategory());
        if (request.getExternalRef() != null) ticket.setExternalRef(request.getExternalRef());

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket updated id={} by={}", saved.getId(), actor.getEmail());
        auditService.record(saved, actor, "UPDATED", null, null);
        return saved;
    }

    @Transactional
    public Ticket changeStatus(Long id, ChangeStatusRequest request, User actor) {
        Ticket ticket = getTicketById(id);
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.getStatus();

        transitionValidator.validate(oldStatus, newStatus);

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket status changed id={} from={} to={} by={}", saved.getId(), oldStatus, newStatus, actor.getEmail());
        auditService.record(saved, actor, "STATUS_CHANGED", oldStatus.name(), newStatus.name());
        return saved;
    }

    @Transactional
    public Ticket assignTicket(Long id, AssignTicketRequest request, User actor) {
        Ticket ticket = getTicketById(id);
        guardClosed(ticket, actor);

        User operator = userRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + request.getOperatorId()));

        String oldAssignee = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getEmail() : null;
        ticket.setAssignedTo(operator);

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket assigned id={} to={} by={}", saved.getId(), operator.getEmail(), actor.getEmail());
        auditService.record(saved, actor, "ASSIGNED", oldAssignee, operator.getEmail());
        return saved;
    }

    @Transactional
    public TicketNote addNote(Long id, AddNoteRequest request, User author) {
        Ticket ticket = getTicketById(id);
        guardClosed(ticket, author);

        TicketNote note = TicketNote.builder()
                .ticket(ticket)
                .author(author)
                .body(request.getBody())
                .visibility(NoteVisibility.INTERNAL)
                .build();

        return noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<TicketNote> getNotes(Long ticketId) {
        getTicketById(ticketId); // verify ticket exists
        return noteRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional
    public Ticket closeTicket(Long id, User actor) {
        Ticket ticket = getTicketById(id);
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.CLOSED);

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket closed id={} by={}", saved.getId(), actor.getEmail());
        auditService.record(saved, actor, "CLOSED", oldStatus.name(), TicketStatus.CLOSED.name());
        return saved;
    }

    private void guardClosed(Ticket ticket, User actor) {
        if (ticket.getStatus() == TicketStatus.CLOSED && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Closed tickets can only be modified by ADMIN");
        }
    }
}
