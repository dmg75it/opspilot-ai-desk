package com.opspilot.desk.ai;

import com.opspilot.desk.entity.Ticket;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.entity.enums.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplatesTest {

    private Ticket buildTicket() {
        User creator = User.builder()
                .id(1L).email("op@example.com").fullName("Operator").role(Role.OPERATOR)
                .active(true).createdAt(LocalDateTime.now()).build();
        return Ticket.builder()
                .id(1L)
                .title("Parcel delivery failed")
                .description("Customer reported that parcel was not delivered despite tracking showing delivered.")
                .status(TicketStatus.IN_PROGRESS)
                .priority(TicketPriority.HIGH)
                .category(TicketCategory.DELIVERY)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Test
    void systemPrompt_isNotBlank() {
        assertThat(PromptTemplates.SYSTEM_PROMPT).isNotBlank();
    }

    @Test
    void buildSummarizePrompt_containsTitle() {
        Ticket ticket = buildTicket();
        String prompt = PromptTemplates.buildSummarizePrompt(ticket);
        assertThat(prompt).contains("Parcel delivery failed");
    }

    @Test
    void buildSummarizePrompt_containsStatus() {
        Ticket ticket = buildTicket();
        String prompt = PromptTemplates.buildSummarizePrompt(ticket);
        assertThat(prompt).contains("IN_PROGRESS");
    }

    @Test
    void buildSuggestReplyPrompt_isNotBlank() {
        Ticket ticket = buildTicket();
        String prompt = PromptTemplates.buildSuggestReplyPrompt(ticket);
        assertThat(prompt).isNotBlank();
    }

    @Test
    void buildSuggestReplyPrompt_containsTitle() {
        Ticket ticket = buildTicket();
        String prompt = PromptTemplates.buildSuggestReplyPrompt(ticket);
        assertThat(prompt).contains("Parcel delivery failed");
    }

    @Test
    void buildClassifyPrompt_containsDescription() {
        Ticket ticket = buildTicket();
        String prompt = PromptTemplates.buildClassifyPrompt(ticket);
        assertThat(prompt).contains("Customer reported");
    }
}
