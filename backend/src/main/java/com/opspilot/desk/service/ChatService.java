package com.opspilot.desk.service;

import com.opspilot.desk.ai.AiChatProvider;
import com.opspilot.desk.ai.AiMessage;
import com.opspilot.desk.ai.AiResponse;
import com.opspilot.desk.ai.PromptTemplates;
import com.opspilot.desk.dto.chat.SendMessageRequest;
import com.opspilot.desk.entity.*;
import com.opspilot.desk.entity.enums.MessageRole;
import com.opspilot.desk.entity.enums.NoteVisibility;
import com.opspilot.desk.exception.AiProviderException;
import com.opspilot.desk.exception.TicketNotFoundException;
import com.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final TicketNoteRepository noteRepository;
    private final AiChatProvider aiProvider;

    @Transactional
    public ChatSession getOrCreateSession(Long ticketId, User user) {
        return sessionRepository.findByTicketId(ticketId)
                .orElseGet(() -> {
                    Ticket ticket = ticketRepository.findById(ticketId)
                            .orElseThrow(() -> new TicketNotFoundException(ticketId));
                    log.info("Creating chat session ticketId={} user={}", ticketId, user.getEmail());
                    return sessionRepository.save(ChatSession.builder()
                            .ticket(ticket)
                            .createdBy(user)
                            .build());
                });
    }

    @Transactional
    public List<ChatMessage> sendMessage(Long ticketId, SendMessageRequest request, User user) {
        ChatSession session = getOrCreateSession(ticketId, user);
        Ticket ticket = session.getTicket();

        ChatMessage userMessage = messageRepository.save(ChatMessage.builder()
                .session(session)
                .role(MessageRole.USER)
                .content(request.getContent())
                .build());

        List<AiMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM)
                .map(m -> new AiMessage(m.getRole().name().toLowerCase(), m.getContent()))
                .collect(Collectors.toList());

        ChatMessage assistantMessage;
        try {
            AiResponse aiResponse = aiProvider.chat(PromptTemplates.SYSTEM_PROMPT, history);
            assistantMessage = messageRepository.save(ChatMessage.builder()
                    .session(session)
                    .role(MessageRole.ASSISTANT)
                    .content(aiResponse.getContent())
                    .model(aiResponse.getModel())
                    .tokenCount(aiResponse.getTokenCount())
                    .build());
        } catch (AiProviderException e) {
            assistantMessage = messageRepository.save(ChatMessage.builder()
                    .session(session)
                    .role(MessageRole.ASSISTANT)
                    .content("AI service is temporarily unavailable.")
                    .errorFlag(true)
                    .errorMessage(e.getMessage())
                    .build());
        }

        return List.of(userMessage, assistantMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> listMessages(Long ticketId) {
        ChatSession session = sessionRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
    }

    @Transactional
    public ChatMessage generateSummary(Long ticketId, User user) {
        ChatSession session = getOrCreateSession(ticketId, user);
        Ticket ticket = session.getTicket();

        AiResponse response = aiProvider.chat(
                PromptTemplates.SYSTEM_PROMPT,
                List.of(new AiMessage("user", PromptTemplates.buildSummarizePrompt(ticket)))
        );

        return messageRepository.save(ChatMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(response.getContent())
                .model(response.getModel())
                .tokenCount(response.getTokenCount())
                .build());
    }

    @Transactional
    public ChatMessage generateSuggestedReply(Long ticketId, User user) {
        ChatSession session = getOrCreateSession(ticketId, user);
        Ticket ticket = session.getTicket();

        AiResponse response = aiProvider.chat(
                PromptTemplates.SYSTEM_PROMPT,
                List.of(new AiMessage("user", PromptTemplates.buildSuggestReplyPrompt(ticket)))
        );

        return messageRepository.save(ChatMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(response.getContent())
                .model(response.getModel())
                .tokenCount(response.getTokenCount())
                .build());
    }

    @Transactional
    public TicketNote applySummaryAsNote(Long ticketId, Long messageId, User user) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        return noteRepository.save(TicketNote.builder()
                .ticket(ticket)
                .author(user)
                .body(message.getContent())
                .visibility(NoteVisibility.AI_SUMMARY)
                .build());
    }
}
