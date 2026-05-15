package io.opspilot.desk.service;

import io.opspilot.desk.ai.AiClient;
import io.opspilot.desk.ai.AiMessage;
import io.opspilot.desk.ai.AiResponse;
import io.opspilot.desk.ai.PromptTemplates;
import io.opspilot.desk.dto.chat.ChatMessageResponse;
import io.opspilot.desk.dto.chat.ChatSessionResponse;
import io.opspilot.desk.dto.chat.SendMessageRequest;
import io.opspilot.desk.dto.ticket.NoteResponse;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.exception.BusinessException;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final TicketNoteRepository noteRepository;
    private final AiClient aiClient;

    @Transactional
    public ChatSessionResponse getOrCreateSession(UUID ticketId) {
        return sessionRepository.findByTicketId(ticketId)
                .map(ChatSessionResponse::from)
                .orElseGet(() -> {
                    Ticket ticket = ticketRepository.findById(ticketId)
                            .orElseThrow(() -> BusinessException.notFound("Ticket not found: " + ticketId));
                    var session = ChatSession.builder().ticket(ticket).build();
                    return ChatSessionResponse.from(sessionRepository.save(session));
                });
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(UUID ticketId) {
        ChatSession session = findSessionOrThrow(ticketId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID ticketId, SendMessageRequest req, User user) {
        ChatSession session = findOrCreateSession(ticketId);
        Ticket ticket = session.getTicket();

        // Save user message
        ChatMessage userMsg = saveMessage(session, MessageRole.USER, req.content(), null, null, false, null);

        // Build conversation for AI
        List<AiMessage> messages = buildConversation(ticket, session);

        return callAiAndSave(session, messages);
    }

    @Transactional
    public ChatMessageResponse generateSummary(UUID ticketId) {
        ChatSession session = findOrCreateSession(ticketId);
        Ticket ticket = session.getTicket();

        List<AiMessage> messages = List.of(
                PromptTemplates.systemMessage(),
                new AiMessage("user", PromptTemplates.summaryPrompt(ticket))
        );
        return callAiAndSave(session, messages);
    }

    @Transactional
    public ChatMessageResponse generateSuggestedReply(UUID ticketId) {
        ChatSession session = findOrCreateSession(ticketId);
        Ticket ticket = session.getTicket();

        List<AiMessage> messages = List.of(
                PromptTemplates.systemMessage(),
                new AiMessage("user", PromptTemplates.suggestedReplyPrompt(ticket))
        );
        return callAiAndSave(session, messages);
    }

    @Transactional
    public NoteResponse applyAsSummaryNote(UUID ticketId, UUID messageId, User user) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found: " + messageId));
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> BusinessException.notFound("Ticket not found: " + ticketId));

        TicketNote note = TicketNote.builder()
                .ticket(ticket)
                .author(user)
                .body(message.getContent())
                .visibility(NoteVisibility.AI_SUMMARY)
                .build();
        return NoteResponse.from(noteRepository.save(note));
    }

    private ChatMessageResponse callAiAndSave(ChatSession session, List<AiMessage> messages) {
        try {
            AiResponse aiResponse = aiClient.chat(messages);
            return ChatMessageResponse.from(saveMessage(
                    session, MessageRole.ASSISTANT, aiResponse.content(),
                    aiResponse.model(), aiResponse.tokensUsed(), false, null));
        } catch (Exception e) {
            log.error("AI call failed for session {}: {}", session.getId(), e.getMessage());
            return ChatMessageResponse.from(saveMessage(
                    session, MessageRole.ASSISTANT,
                    "AI service unavailable. Please try again later.",
                    null, null, true, e.getMessage()));
        }
    }

    private List<AiMessage> buildConversation(Ticket ticket, ChatSession session) {
        var history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<AiMessage> messages = new ArrayList<>();
        messages.add(PromptTemplates.systemMessage());
        messages.add(new AiMessage("user", PromptTemplates.buildTicketContext(ticket)));
        for (ChatMessage m : history) {
            messages.add(new AiMessage(m.getRole().name().toLowerCase(), m.getContent()));
        }
        return messages;
    }

    private ChatMessage saveMessage(ChatSession session, MessageRole role, String content,
                                    String model, Integer tokens, boolean error, String errorMsg) {
        var msg = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .model(model)
                .tokenEstimate(tokens)
                .errorFlag(error)
                .errorMessage(errorMsg)
                .build();
        return messageRepository.save(msg);
    }

    private ChatSession findSessionOrThrow(UUID ticketId) {
        return sessionRepository.findByTicketId(ticketId)
                .orElseThrow(() -> BusinessException.notFound("Chat session not found for ticket: " + ticketId));
    }

    private ChatSession findOrCreateSession(UUID ticketId) {
        return sessionRepository.findByTicketId(ticketId).orElseGet(() -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> BusinessException.notFound("Ticket not found: " + ticketId));
            return sessionRepository.save(ChatSession.builder().ticket(ticket).build());
        });
    }
}
