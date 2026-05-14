package com.opspilot.desk.controller;

import com.opspilot.desk.dto.*;
import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.repository.UserRepository;
import com.opspilot.desk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody TicketRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        AppUser user = getUser(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(req, user));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TicketSummaryResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ticketService.list(status, priority, category, assignedTo, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TicketUpdateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ticketService.update(id, req, getUser(principal)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TicketStatusRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ticketService.changeStatus(id, req, getUser(principal)));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable UUID id,
            @RequestBody TicketAssignRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ticketService.assign(id, req, getUser(principal)));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<NoteResponse> addNote(
            @PathVariable UUID id,
            @Valid @RequestBody NoteRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ticketService.addNote(id, req, getUser(principal)));
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<List<NoteResponse>> getNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getNotes(id));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<?> getAudit(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getAudit(id));
    }

    private AppUser getUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }
}
