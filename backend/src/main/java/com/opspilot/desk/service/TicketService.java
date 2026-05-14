package com.opspilot.desk.service;

import com.opspilot.desk.dto.*;
import com.opspilot.desk.entity.*;
import com.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketNoteRepository noteRepository;
    private final TicketAuditRepository auditRepository;

    public TicketResponse create(TicketRequest req, AppUser currentUser) {
        if (req.externalRef() != null && !req.externalRef().isBlank()
                && ticketRepository.existsByExternalRef(req.externalRef())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "External ref already exists");
        }
        Ticket ticket = Ticket.builder()
            .externalRef(req.externalRef())
            .title(req.title())
            .description(req.description())
            .status(TicketStatus.NEW)
            .priority(TicketPriority.valueOf(req.priority()))
            .category(TicketCategory.valueOf(req.category()))
            .createdBy(currentUser)
            .build();
        ticket = ticketRepository.save(ticket);
        audit(ticket, "CREATED", null, null, null, currentUser);
        log.info("Ticket created: {} by {}", ticket.getId(), currentUser.getEmail());
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(String status, String priority, String category,
                                                     UUID assignedTo, int page, int size) {
        Specification<Ticket> spec = Specification.where(null);
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), TicketStatus.valueOf(status)));
        if (priority != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("priority"), TicketPriority.valueOf(priority)));
        if (category != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("category"), TicketCategory.valueOf(category)));
        if (assignedTo != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("assignedTo").get("id"), assignedTo));

        Page<Ticket> pageResult = ticketRepository.findAll(spec,
            PageRequest.of(page, size, Sort.by("updatedAt").descending()));
        return new PageResponse<>(
            pageResult.getContent().stream().map(this::toSummary).toList(),
            page, size, pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast()
        );
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(UUID id) {
        return toResponse(findTicket(id));
    }

    public TicketResponse update(UUID id, TicketUpdateRequest req, AppUser currentUser) {
        Ticket ticket = findTicket(id);
        if (ticket.getStatus() == TicketStatus.CLOSED && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new AccessDeniedException("Closed tickets can only be edited by ADMIN");
        }
        if (req.title() != null) ticket.setTitle(req.title());
        if (req.description() != null) ticket.setDescription(req.description());
        if (req.priority() != null) ticket.setPriority(TicketPriority.valueOf(req.priority()));
        if (req.category() != null) ticket.setCategory(TicketCategory.valueOf(req.category()));
        if (req.externalRef() != null) ticket.setExternalRef(req.externalRef());
        ticket = ticketRepository.save(ticket);
        audit(ticket, "UPDATED", null, null, null, currentUser);
        log.info("Ticket updated: {} by {}", id, currentUser.getEmail());
        return toResponse(ticket);
    }

    public TicketResponse changeStatus(UUID id, TicketStatusRequest req, AppUser currentUser) {
        Ticket ticket = findTicket(id);
        TicketStatus newStatus = TicketStatus.valueOf(req.status());
        validateTransition(ticket.getStatus(), newStatus);
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now());
        }
        ticket = ticketRepository.save(ticket);
        audit(ticket, "STATUS_CHANGED", "status", oldStatus.name(), newStatus.name(), currentUser);
        log.info("Ticket {} status changed: {} -> {} by {}", id, oldStatus, newStatus, currentUser.getEmail());
        return toResponse(ticket);
    }

    public TicketResponse assign(UUID id, TicketAssignRequest req, AppUser currentUser) {
        Ticket ticket = findTicket(id);
        AppUser operator = req.operatorId() != null
            ? userRepository.findById(req.operatorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operator not found"))
            : null;
        AppUser previousAssignee = ticket.getAssignedTo();
        ticket.setAssignedTo(operator);
        ticket = ticketRepository.save(ticket);
        audit(ticket, "ASSIGNED", "assignedTo",
            previousAssignee != null ? previousAssignee.getEmail() : null,
            operator != null ? operator.getEmail() : null, currentUser);
        log.info("Ticket {} assigned to {} by {}", id,
            operator != null ? operator.getEmail() : "nobody", currentUser.getEmail());
        return toResponse(ticket);
    }

    public NoteResponse addNote(UUID ticketId, NoteRequest req, AppUser currentUser) {
        Ticket ticket = findTicket(ticketId);
        TicketNote note = TicketNote.builder()
            .ticket(ticket)
            .author(currentUser)
            .body(req.body())
            .visibility(NoteVisibility.INTERNAL)
            .build();
        note = noteRepository.save(note);
        log.info("Note added to ticket {} by {}", ticketId, currentUser.getEmail());
        return toNoteResponse(note);
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotes(UUID ticketId) {
        findTicket(ticketId);
        return noteRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
            .stream().map(this::toNoteResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketAudit> getAudit(UUID ticketId) {
        return auditRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    public Ticket findTicket(UUID id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private void audit(Ticket ticket, String action, String field, String oldVal, String newVal, AppUser actor) {
        auditRepository.save(TicketAudit.builder()
            .ticketId(ticket.getId())
            .action(action)
            .fieldName(field)
            .oldValue(oldVal)
            .newValue(newVal)
            .actorId(actor.getId())
            .build());
    }

    private void validateTransition(TicketStatus from, TicketStatus to) {
        boolean valid = switch (from) {
            case NEW -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.CLOSED;
            case IN_PROGRESS -> to == TicketStatus.WAITING_FOR_CUSTOMER
                || to == TicketStatus.RESOLVED || to == TicketStatus.CLOSED;
            case WAITING_FOR_CUSTOMER -> to == TicketStatus.IN_PROGRESS
                || to == TicketStatus.RESOLVED || to == TicketStatus.CLOSED;
            case RESOLVED -> to == TicketStatus.CLOSED || to == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid status transition: " + from + " -> " + to);
        }
    }

    public TicketResponse toResponse(Ticket t) {
        return new TicketResponse(
            t.getId(), t.getExternalRef(), t.getTitle(), t.getDescription(),
            t.getStatus().name(), t.getPriority().name(), t.getCategory().name(),
            t.getAssignedTo() != null ? toUserDto(t.getAssignedTo()) : null,
            toUserDto(t.getCreatedBy()),
            t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(), t.getVersion()
        );
    }

    public TicketSummaryResponse toSummary(Ticket t) {
        return new TicketSummaryResponse(
            t.getId(), t.getExternalRef(), t.getTitle(),
            t.getStatus().name(), t.getPriority().name(), t.getCategory().name(),
            t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null,
            t.getCreatedBy().getFullName(),
            t.getCreatedAt(), t.getUpdatedAt()
        );
    }

    private NoteResponse toNoteResponse(TicketNote n) {
        return new NoteResponse(
            n.getId(), n.getTicket().getId(),
            n.getAuthor() != null ? n.getAuthor().getFullName() : "System",
            n.getBody(), n.getVisibility().name(), n.getCreatedAt()
        );
    }

    private UserDto toUserDto(AppUser u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }
}
