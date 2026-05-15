package io.opspilot.desk.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PromptBuilderTest {
    @Test
    void allTemplatesNonBlank() {
        assertThat(PromptTemplates.SYSTEM_BASE).isNotBlank();
        assertThat(PromptTemplates.SUMMARIZE_TICKET).isNotBlank();
        assertThat(PromptTemplates.SUGGEST_NEXT_ACTION).isNotBlank();
        assertThat(PromptTemplates.DRAFT_CUSTOMER_REPLY).isNotBlank();
        assertThat(PromptTemplates.IDENTIFY_MISSING_INFO).isNotBlank();
        assertThat(PromptTemplates.CLASSIFY_PRIORITY_CATEGORY).isNotBlank();
    }

    @Test
    void versionTagPresent() {
        assertThat(PromptTemplates.VERSION).isNotBlank();
    }
}
