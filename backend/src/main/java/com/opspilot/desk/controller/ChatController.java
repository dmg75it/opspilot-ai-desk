package com.opspilot.desk.controller;

import com.opspilot.desk.dto.*;
import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.repository.UserRepository;
import com.opspilot.desk.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @PostMapping("/tickets/{ticketId}/chat/session")
    public ResponseEntity<ChatSessionResponse> getOrCreateSession(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.getOrCreateSession(ticketId, getUser(principal)));
    }

    @PostMapping("/chat/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatMessageRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.sendMessage(sessionId, req, getUser(principal)));
    }

    @GetMapping("/chat/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(chatService.getMessages(sessionId));
    }

    @PostMapping("/tickets/{ticketId}/chat/summary")
    public ResponseEntity<ChatMessageResponse> generateSummary(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.generateSummary(ticketId, getUser(principal)));
    }

    @PostMapping("/tickets/{ticketId}/chat/suggest-reply")
    public ResponseEntity<ChatMessageResponse> suggestReply(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.generateSuggestedReply(ticketId, getUser(principal)));
    }

    @PostMapping("/tickets/{ticketId}/notes/from-ai/{messageId}")
    public ResponseEntity<NoteResponse> applyAiSummary(
            @PathVariable UUID ticketId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.applyAiSummary(ticketId, messageId, getUser(principal)));
    }

    private AppUser getUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }
}
