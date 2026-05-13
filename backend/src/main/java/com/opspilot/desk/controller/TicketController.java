package com.opspilot.desk.controller;

import com.opspilot.desk.dto.ticket.*;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> list(TicketFilterParams params) {
        return ResponseEntity.ok(ticketService.listTickets(params).map(TicketResponse::from));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketResponse.from(ticketService.createTicket(request, user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(TicketResponse.from(ticketService.getTicketById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateTicketRequest request,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(TicketResponse.from(ticketService.updateTicket(id, request, user)));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<TicketResponse> changeStatus(@PathVariable Long id,
                                                        @Valid @RequestBody ChangeStatusRequest request,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(TicketResponse.from(ticketService.changeStatus(id, request, user)));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assign(@PathVariable Long id,
                                                  @Valid @RequestBody AssignTicketRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(TicketResponse.from(ticketService.assignTicket(id, request, user)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<TicketResponse> close(@PathVariable Long id,
                                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(TicketResponse.from(ticketService.closeTicket(id, user)));
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<List<TicketNoteResponse>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getNotes(id).stream()
                .map(TicketNoteResponse::from).toList());
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<TicketNoteResponse> addNote(@PathVariable Long id,
                                                       @Valid @RequestBody AddNoteRequest request,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketNoteResponse.from(ticketService.addNote(id, request, user)));
    }
}
