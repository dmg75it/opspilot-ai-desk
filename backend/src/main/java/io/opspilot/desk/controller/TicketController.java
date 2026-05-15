package io.opspilot.desk.controller;

import io.opspilot.desk.dto.ticket.*;
import io.opspilot.desk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ticketService.create(req, user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Sort.Direction direction = Sort.Direction.fromString(dir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        return ResponseEntity.ok(ticketService.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.update(id, req, user.getUsername()));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<TicketResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.changeStatus(id, req, user.getUsername()));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable UUID id,
            @RequestBody AssignTicketRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.assign(id, req, user.getUsername()));
    }
}
