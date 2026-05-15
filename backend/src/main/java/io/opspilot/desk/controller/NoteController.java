package io.opspilot.desk.controller;

import io.opspilot.desk.dto.note.*;
import io.opspilot.desk.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteResponse> addNote(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateNoteRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(noteService.addNote(ticketId, req, user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> listNotes(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(noteService.listNotes(ticketId));
    }
}
