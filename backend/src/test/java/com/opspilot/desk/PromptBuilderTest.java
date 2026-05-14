package com.opspilot.desk;

import com.opspilot.desk.integration.PromptTemplates;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PromptBuilderTest {

    @Test
    void summarizePrompt_containsTicketInfo() {
        String prompt = PromptTemplates.summarizeTicket("Delivery issue", "Package lost", "NEW", "HIGH");
        assertThat(prompt).contains("Delivery issue");
        assertThat(prompt).contains("Package lost");
        assertThat(prompt).contains("HIGH");
        assertThat(prompt).contains(PromptTemplates.VERSION);
    }

    @Test
    void suggestNextAction_containsStatus() {
        String prompt = PromptTemplates.suggestNextAction("Issue", "Description", "IN_PROGRESS");
        assertThat(prompt).contains("IN_PROGRESS");
        assertThat(prompt).contains(PromptTemplates.VERSION);
    }

    @Test
    void draftReply_containsTitle() {
        String prompt = PromptTemplates.draftCustomerReply("Lost Package", "Customer lost package");
        assertThat(prompt).contains("Lost Package");
    }
}
