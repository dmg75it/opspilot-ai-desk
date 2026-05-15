package io.opspilot.desk.service;

import io.opspilot.desk.dto.ticket.*;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.exception.BusinessException;
import io.opspilot.desk.repository.TicketNoteRepository;
import io.opspilot.desk.repository.TicketRepository;
import io.opspilot.desk.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketNoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketResponse create(CreateTicketRequest req, User creator) {
        if (StringUtils.hasText(req.externalRef()) && ticketRepository.existsByExternalRef(req.externalRef())) {
            throw BusinessException.conflict("External ref already exists: " + req.externalRef());
        }
        var ticket = Ticket.builder()
                .externalRef(req.externalRef())
                .title(req.title())
                .description(req.description())
                .status(TicketStatus.NEW)
                .priority(req.priority())
                .category(req.category())
                .createdBy(creator)
                .build();
        ticket = ticketRepository.save(ticket);
        addSystemNote(ticket, creator, "Ticket created by " + creator.getEmail());
        log.info("Ticket created: id={} title={} by={}", ticket.getId(), ticket.getTitle(), creator.getEmail());
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(String status, String priority, String category, Pageable pageable) {
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), TicketStatus.valueOf(status)));
            }
            if (StringUtils.hasText(priority)) {
                predicates.add(cb.equal(root.get("priority"), TicketPriority.valueOf(priority)));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), TicketCategory.valueOf(category)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ticketRepository.findAll(spec, pageable).map(TicketResponse::from);
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(UUID id) {
        return TicketResponse.from(findTicketOrThrow(id));
    }

    @Transactional
    public TicketResponse update(UUID id, UpdateTicketRequest req, User principal) {
        Ticket ticket = findTicketOrThrow(id);
        checkNotClosedOrAdmin(ticket, principal);

        if (StringUtils.hasText(req.externalRef()) && !req.externalRef().equals(ticket.getExternalRef())) {
            if (ticketRepository.existsByExternalRef(req.externalRef())) {
                throw BusinessException.conflict("External ref already exists: " + req.externalRef());
            }
        }
        if (StringUtils.hasText(req.title())) ticket.setTitle(req.title());
        if (StringUtils.hasText(req.description())) ticket.setDescription(req.description());
        if (req.priority() != null) ticket.setPriority(req.priority());
        if (req.category() != null) ticket.setCategory(req.category());
        if (req.externalRef() != null) ticket.setExternalRef(req.externalRef());

        log.info("Ticket updated: id={} by={}", id, principal.getEmail());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse changeStatus(UUID id, ChangeStatusRequest req, User principal) {
        Ticket ticket = findTicketOrThrow(id);
        TicketStatus current = ticket.getStatus();
        TicketStatus target = req.status();

        if (current == TicketStatus.CLOSED && principal.getRole() != Role.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can change status of closed tickets");
        }
        if (current != TicketStatus.CLOSED && !current.canTransitionTo(target)) {
            throw BusinessException.badRequest(
                    "Invalid transition: " + current + " -> " + target);
        }

        ticket.setStatus(target);
        if (target == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }

        addSystemNote(ticket, principal,
                "Status changed from " + current + " to " + target + " by " + principal.getEmail());
        log.info("Ticket status changed: id={} {}->{}  by={}", id, current, target, principal.getEmail());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assign(UUID id, AssignTicketRequest req, User principal) {
        Ticket ticket = findTicketOrThrow(id);
        checkNotClosedOrAdmin(ticket, principal);

        User assignee = null;
        if (req.operatorId() != null) {
            assignee = userRepository.findById(req.operatorId())
                    .orElseThrow(() -> BusinessException.notFound("User not found: " + req.operatorId()));
        }
        ticket.setAssignedTo(assignee);
        String note = assignee != null
                ? "Ticket assigned to " + assignee.getEmail()
                : "Ticket unassigned";
        addSystemNote(ticket, principal, note);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public NoteResponse addNote(UUID id, AddNoteRequest req, User author) {
        Ticket ticket = findTicketOrThrow(id);
        checkNotClosedOrAdmin(ticket, author);

        TicketNote note = TicketNote.builder()
                .ticket(ticket)
                .author(author)
                .body(req.body())
                .visibility(NoteVisibility.INTERNAL)
                .build();
        return NoteResponse.from(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listNotes(UUID ticketId) {
        findTicketOrThrow(ticketId);
        return noteRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse close(UUID id, User principal) {
        Ticket ticket = findTicketOrThrow(id);
        ticket.setStatus(TicketStatus.CLOSED);
        addSystemNote(ticket, principal, "Ticket closed by " + principal.getEmail());
        log.info("Ticket closed: id={} by={}", id, principal.getEmail());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Ticket not found: " + id));
    }

    private void checkNotClosedOrAdmin(Ticket ticket, User principal) {
        if (ticket.getStatus() == TicketStatus.CLOSED && principal.getRole() != Role.ADMIN) {
            throw BusinessException.forbidden("Closed tickets can only be edited by ADMIN");
        }
    }

    private void addSystemNote(Ticket ticket, User actor, String body) {
        TicketNote note = TicketNote.builder()
                .ticket(ticket)
                .author(actor)
                .body(body)
                .visibility(NoteVisibility.SYSTEM)
                .build();
        noteRepository.save(note);
    }
}
