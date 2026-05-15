package io.opspilot.desk.controller;

import io.opspilot.desk.dto.ticket.*;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TicketResponse create(@Valid @RequestBody CreateTicketRequest request,
                          @AuthenticationPrincipal User user) {
        return ticketService.create(request, user);
    }

    @GetMapping
    Page<TicketResponse> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String priority,
                               @RequestParam(required = false) String category,
                               @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ticketService.list(status, priority, category, pageable);
    }

    @GetMapping("/{id}")
    TicketResponse getById(@PathVariable UUID id) {
        return ticketService.getById(id);
    }

    @PatchMapping("/{id}")
    TicketResponse update(@PathVariable UUID id,
                           @Valid @RequestBody UpdateTicketRequest request,
                           @AuthenticationPrincipal User user) {
        return ticketService.update(id, request, user);
    }

    @PatchMapping("/{id}/status")
    TicketResponse changeStatus(@PathVariable UUID id,
                                 @Valid @RequestBody ChangeStatusRequest request,
                                 @AuthenticationPrincipal User user) {
        return ticketService.changeStatus(id, request, user);
    }

    @PatchMapping("/{id}/assign")
    TicketResponse assign(@PathVariable UUID id,
                           @RequestBody AssignTicketRequest request,
                           @AuthenticationPrincipal User user) {
        return ticketService.assign(id, request, user);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    NoteResponse addNote(@PathVariable UUID id,
                          @Valid @RequestBody AddNoteRequest request,
                          @AuthenticationPrincipal User user) {
        return ticketService.addNote(id, request, user);
    }

    @GetMapping("/{id}/notes")
    List<NoteResponse> listNotes(@PathVariable UUID id) {
        return ticketService.listNotes(id);
    }

    @PostMapping("/{id}/close")
    TicketResponse close(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ticketService.close(id, user);
    }
}
