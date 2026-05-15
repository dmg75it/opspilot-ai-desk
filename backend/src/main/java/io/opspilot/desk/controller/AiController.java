package io.opspilot.desk.controller;

import io.opspilot.desk.dto.ai.*;
import io.opspilot.desk.dto.note.NoteResponse;
import io.opspilot.desk.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

    @GetMapping("/session")
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(aiService.getOrCreateSession(ticketId));
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(aiService.sendMessage(ticketId, req.content()));
    }

    @PostMapping("/summary")
    public ResponseEntity<AiActionResponse> generateSummary(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(aiService.generateSummary(ticketId));
    }

    @PostMapping("/suggested-reply")
    public ResponseEntity<AiActionResponse> suggestedReply(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(aiService.generateSuggestedReply(ticketId));
    }

    @PostMapping("/apply-summary")
    public ResponseEntity<NoteResponse> applySummary(
            @PathVariable UUID ticketId,
            @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(aiService.applyAiSummaryAsNote(ticketId, req.content()));
    }
}
