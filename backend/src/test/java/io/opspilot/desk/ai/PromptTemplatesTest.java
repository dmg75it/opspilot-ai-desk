package io.opspilot.desk.ai;

import io.opspilot.desk.entity.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplatesTest {

    @Test
    void systemPromptIsNotEmpty() {
        assertThat(PromptTemplates.SYSTEM_PROMPT).isNotBlank();
        assertThat(PromptTemplates.systemMessage().role()).isEqualTo("system");
    }

    @Test
    void ticketContextContainsAllFields() {
        Ticket ticket = buildTicket();
        String context = PromptTemplates.buildTicketContext(ticket);
        assertThat(context)
                .contains(ticket.getId().toString())
                .contains(ticket.getTitle())
                .contains(ticket.getStatus().name())
                .contains(ticket.getPriority().name())
                .contains(ticket.getCategory().name());
    }

    @Test
    void summaryPromptContainsTicketInfo() {
        Ticket ticket = buildTicket();
        String prompt = PromptTemplates.summaryPrompt(ticket);
        assertThat(prompt).contains("summary").contains(ticket.getTitle());
    }

    @Test
    void suggestedReplyPromptContainsTicketInfo() {
        Ticket ticket = buildTicket();
        String prompt = PromptTemplates.suggestedReplyPrompt(ticket);
        assertThat(prompt).contains("reply").contains(ticket.getTitle());
    }

    private Ticket buildTicket() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.OPERATOR)
                .build();
        return Ticket.builder()
                .id(UUID.randomUUID())
                .title("Package lost in transit")
                .description("Customer reports package has not arrived after 5 days.")
                .status(TicketStatus.IN_PROGRESS)
                .priority(TicketPriority.HIGH)
                .category(TicketCategory.DELIVERY)
                .createdBy(user)
                .build();
    }
}
