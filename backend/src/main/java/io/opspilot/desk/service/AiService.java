package io.opspilot.desk.service;

import io.opspilot.desk.ai.*;
import io.opspilot.desk.dto.ai.*;
import io.opspilot.desk.dto.note.NoteResponse;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {
    private final AiClient aiClient;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final NoteService noteService;

    @Transactional
    public ChatSessionResponse getOrCreateSession(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var session = sessionRepository.findByTicket(ticket)
            .orElseGet(() -> {
                var s = new ChatSession();
                s.setTicket(ticket);
                return sessionRepository.save(s);
            });
        var messages = messageRepository.findBySessionOrderByCreatedAtAsc(session)
            .stream().map(this::toMessageResponse).toList();
        return new ChatSessionResponse(session.getId(), ticketId, session.getCreatedAt(), messages);
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID ticketId, String userContent) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var session = sessionRepository.findByTicket(ticket).orElseGet(() -> {
            var s = new ChatSession();
            s.setTicket(ticket);
            return sessionRepository.save(s);
        });

        var userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole(ChatMessage.Role.USER);
        userMsg.setContent(userContent);
        messageRepository.save(userMsg);

        var history = messageRepository.findBySessionOrderByCreatedAtAsc(session);
        var requestMessages = history.stream()
            .map(m -> new AiRequest.Message(m.getRole().name().toLowerCase(), m.getContent()))
            .toList();

        var ticketContext = buildTicketContext(ticket);
        var aiRequest = new AiRequest(PromptTemplates.SYSTEM_BASE + "\n\nTicket context:\n" + ticketContext, requestMessages);
        var aiResponse = aiClient.chat(aiRequest);

        var assistantMsg = new ChatMessage();
        assistantMsg.setSession(session);
        assistantMsg.setRole(ChatMessage.Role.ASSISTANT);
        assistantMsg.setContent(aiResponse.isError() ? "" : aiResponse.content());
        assistantMsg.setModel(aiResponse.model());
        assistantMsg.setPromptTokens(aiResponse.promptTokens());
        assistantMsg.setCompletionTokens(aiResponse.completionTokens());
        assistantMsg.setError(aiResponse.isError());
        assistantMsg.setErrorMessage(aiResponse.errorMessage());
        return toMessageResponse(messageRepository.save(assistantMsg));
    }

    @Transactional
    public AiActionResponse generateSummary(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var context = buildTicketContext(ticket);
        var request = new AiRequest(PromptTemplates.SYSTEM_BASE,
            List.of(new AiRequest.Message("user", PromptTemplates.SUMMARIZE_TICKET + "\n\n" + context)));
        var response = aiClient.chat(request);
        if (response.isError()) return new AiActionResponse(null, false, response.errorMessage());
        return new AiActionResponse(response.content(), true, null);
    }

    @Transactional
    public AiActionResponse generateSuggestedReply(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var context = buildTicketContext(ticket);
        var request = new AiRequest(PromptTemplates.SYSTEM_BASE,
            List.of(new AiRequest.Message("user", PromptTemplates.DRAFT_CUSTOMER_REPLY + "\n\n" + context)));
        var response = aiClient.chat(request);
        if (response.isError()) return new AiActionResponse(null, false, response.errorMessage());
        return new AiActionResponse(response.content(), true, null);
    }

    @Transactional
    public NoteResponse applyAiSummaryAsNote(UUID ticketId, String content) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        return noteService.addAiSummaryNote(ticket, content);
    }

    public long countAiInteractionsToday() {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return messageRepository.countAssistantMessagesSince(startOfDay);
    }

    private String buildTicketContext(Ticket t) {
        return "Title: " + t.getTitle() + "\n" +
            "Description: " + t.getDescription() + "\n" +
            "Status: " + t.getStatus() + "\n" +
            "Priority: " + t.getPriority() + "\n" +
            "Category: " + t.getCategory();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getRole().name(), m.getContent(),
            m.getModel(), m.getPromptTokens(), m.getCompletionTokens(),
            m.getCreatedAt(), m.isError(), m.getErrorMessage());
    }
}
