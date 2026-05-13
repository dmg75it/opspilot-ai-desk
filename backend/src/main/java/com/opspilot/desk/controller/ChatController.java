package com.opspilot.desk.controller;

import com.opspilot.desk.dto.chat.ChatMessageResponse;
import com.opspilot.desk.dto.chat.ChatSessionResponse;
import com.opspilot.desk.dto.chat.SendMessageRequest;
import com.opspilot.desk.dto.ticket.TicketNoteResponse;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/session")
    public ResponseEntity<ChatSessionResponse> getOrCreateSession(@PathVariable Long ticketId,
                                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ChatSessionResponse.from(chatService.getOrCreateSession(ticketId, user)));
    }

    @PostMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> sendMessage(@PathVariable Long ticketId,
                                                                   @Valid @RequestBody SendMessageRequest request,
                                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.sendMessage(ticketId, request, user).stream()
                .map(ChatMessageResponse::from).toList());
    }

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> listMessages(@PathVariable Long ticketId) {
        return ResponseEntity.ok(chatService.listMessages(ticketId).stream()
                .map(ChatMessageResponse::from).toList());
    }

    @PostMapping("/summarize")
    public ResponseEntity<ChatMessageResponse> generateSummary(@PathVariable Long ticketId,
                                                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ChatMessageResponse.from(chatService.generateSummary(ticketId, user)));
    }

    @PostMapping("/suggest-reply")
    public ResponseEntity<ChatMessageResponse> generateSuggestedReply(@PathVariable Long ticketId,
                                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ChatMessageResponse.from(chatService.generateSuggestedReply(ticketId, user)));
    }

    @PostMapping("/apply-summary/{messageId}")
    public ResponseEntity<TicketNoteResponse> applySummaryAsNote(@PathVariable Long ticketId,
                                                                   @PathVariable Long messageId,
                                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(TicketNoteResponse.from(chatService.applySummaryAsNote(ticketId, messageId, user)));
    }
}
