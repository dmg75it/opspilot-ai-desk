package io.opspilot.desk.controller;

import io.opspilot.desk.dto.chat.ChatMessageResponse;
import io.opspilot.desk.dto.chat.ChatSessionResponse;
import io.opspilot.desk.dto.chat.SendMessageRequest;
import io.opspilot.desk.dto.ticket.NoteResponse;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/session")
    ChatSessionResponse getOrCreateSession(@PathVariable UUID ticketId) {
        return chatService.getOrCreateSession(ticketId);
    }

    @GetMapping("/messages")
    List<ChatMessageResponse> listMessages(@PathVariable UUID ticketId) {
        return chatService.listMessages(ticketId);
    }

    @PostMapping("/messages")
    ChatMessageResponse sendMessage(@PathVariable UUID ticketId,
                                     @Valid @RequestBody SendMessageRequest request,
                                     @AuthenticationPrincipal User user) {
        return chatService.sendMessage(ticketId, request, user);
    }

    @PostMapping("/summary")
    ChatMessageResponse generateSummary(@PathVariable UUID ticketId) {
        return chatService.generateSummary(ticketId);
    }

    @PostMapping("/suggested-reply")
    ChatMessageResponse generateSuggestedReply(@PathVariable UUID ticketId) {
        return chatService.generateSuggestedReply(ticketId);
    }

    @PostMapping("/messages/{messageId}/apply-as-note")
    NoteResponse applyAsNote(@PathVariable UUID ticketId,
                              @PathVariable UUID messageId,
                              @AuthenticationPrincipal User user) {
        return chatService.applyAsSummaryNote(ticketId, messageId, user);
    }
}
