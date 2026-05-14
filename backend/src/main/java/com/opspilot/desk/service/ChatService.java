package com.opspilot.desk.service;

import com.opspilot.desk.dto.*;
import com.opspilot.desk.entity.*;
import com.opspilot.desk.integration.*;
import com.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final TicketNoteRepository noteRepository;
    private final TicketService ticketService;
    private final AiProvider aiProvider;

    public ChatSessionResponse getOrCreateSession(UUID ticketId, AppUser currentUser) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ChatSession session = sessionRepository
            .findByTicketIdAndUserId(ticketId, currentUser.getId())
            .orElseGet(() -> sessionRepository.save(
                ChatSession.builder().ticket(ticket).user(currentUser).build()
            ));
        return new ChatSessionResponse(session.getId(), ticketId, session.getCreatedAt());
    }

    public ChatMessageResponse sendMessage(UUID sessionId, ChatMessageRequest req, AppUser currentUser) {
        ChatSession session = findSession(sessionId);
        messageRepository.save(ChatMessage.builder()
            .session(session)
            .role(MessageRole.USER)
            .content(req.content())
            .build());

        List<AiMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream()
            .filter(m -> m.getRole() != MessageRole.SYSTEM)
            .map(m -> new AiMessage(m.getRole().name().toLowerCase(), m.getContent()))
            .toList();

        Ticket ticket = session.getTicket();
        String systemPrompt = PromptTemplates.SYSTEM_PROMPT + "\n\nCurrent ticket context:\nTitle: "
            + ticket.getTitle() + "\nStatus: " + ticket.getStatus() + "\nPriority: " + ticket.getPriority();

        log.info("AI request for session {} ticket {}", sessionId, ticket.getId());
        AiResponse aiResp = aiProvider.chat(systemPrompt, history);

        ChatMessage assistantMsg = messageRepository.save(ChatMessage.builder()
            .session(session)
            .role(MessageRole.ASSISTANT)
            .content(aiResp.error() ? "[Error] " + aiResp.errorMessage() : aiResp.content())
            .model(aiResp.model())
            .tokenEstimate(aiResp.tokenEstimate())
            .error(aiResp.error())
            .errorMessage(aiResp.errorMessage())
            .build());

        if (aiResp.error()) {
            log.warn("AI error for session {}: {}", sessionId, aiResp.errorMessage());
        } else {
            log.info("AI response for session {}: model={}, tokens={}", sessionId, aiResp.model(), aiResp.tokenEstimate());
        }

        return toResponse(assistantMsg);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID sessionId) {
        findSession(sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream().map(this::toResponse).toList();
    }

    public ChatMessageResponse generateSummary(UUID ticketId, AppUser currentUser) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ChatSession session = sessionRepository
            .findByTicketIdAndUserId(ticketId, currentUser.getId())
            .orElseGet(() -> sessionRepository.save(
                ChatSession.builder().ticket(ticket).user(currentUser).build()
            ));

        String prompt = PromptTemplates.summarizeTicket(
            ticket.getTitle(), ticket.getDescription(),
            ticket.getStatus().name(), ticket.getPriority().name()
        );
        AiResponse resp = aiProvider.chat(PromptTemplates.SYSTEM_PROMPT,
            List.of(new AiMessage("user", prompt)));

        ChatMessage msg = messageRepository.save(ChatMessage.builder()
            .session(session)
            .role(MessageRole.ASSISTANT)
            .content(resp.error() ? "[Error] " + resp.errorMessage() : resp.content())
            .model(resp.model())
            .tokenEstimate(resp.tokenEstimate())
            .error(resp.error())
            .errorMessage(resp.errorMessage())
            .build());

        return toResponse(msg);
    }

    public ChatMessageResponse generateSuggestedReply(UUID ticketId, AppUser currentUser) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ChatSession session = sessionRepository
            .findByTicketIdAndUserId(ticketId, currentUser.getId())
            .orElseGet(() -> sessionRepository.save(
                ChatSession.builder().ticket(ticket).user(currentUser).build()
            ));

        String prompt = PromptTemplates.draftCustomerReply(ticket.getTitle(), ticket.getDescription());
        AiResponse resp = aiProvider.chat(PromptTemplates.SYSTEM_PROMPT,
            List.of(new AiMessage("user", prompt)));

        ChatMessage msg = messageRepository.save(ChatMessage.builder()
            .session(session)
            .role(MessageRole.ASSISTANT)
            .content(resp.error() ? "[Error] " + resp.errorMessage() : resp.content())
            .model(resp.model())
            .tokenEstimate(resp.tokenEstimate())
            .error(resp.error())
            .errorMessage(resp.errorMessage())
            .build());

        return toResponse(msg);
    }

    public NoteResponse applyAiSummary(UUID ticketId, UUID messageId, AppUser currentUser) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ChatMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        TicketNote note = noteRepository.save(TicketNote.builder()
            .ticket(ticket)
            .author(currentUser)
            .body("[AI Summary - " + PromptTemplates.VERSION + "]\n\n" + message.getContent())
            .visibility(NoteVisibility.AI_SUMMARY)
            .build());

        log.info("AI summary applied to ticket {} by {}", ticketId, currentUser.getEmail());
        return new NoteResponse(
            note.getId(), ticketId,
            currentUser.getFullName(), note.getBody(),
            note.getVisibility().name(), note.getCreatedAt()
        );
    }

    private ChatSession findSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        return new ChatMessageResponse(
            m.getId(), m.getSession().getId(), m.getRole().name(),
            m.getContent(), m.getModel(), m.getTokenEstimate(),
            m.isError(), m.getErrorMessage(), m.getCreatedAt()
        );
    }
}
