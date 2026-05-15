package io.opspilot.desk.ai;

import io.opspilot.desk.entity.Ticket;

/** Versioned prompt templates. Version: v1 */
public final class PromptTemplates {

    public static final String SYSTEM_PROMPT = """
            You are an expert AI assistant for a transport and logistics operations support desk.
            You help operators handle field issues, tickets, and customer communications.
            Be concise, professional, and action-oriented.
            Never modify ticket status on your own. Always present suggestions for operator review.
            """;

    public static AiMessage systemMessage() {
        return new AiMessage("system", SYSTEM_PROMPT);
    }

    public static String buildTicketContext(Ticket ticket) {
        return String.format("""
                Ticket context:
                - ID: %s
                - Title: %s
                - Status: %s
                - Priority: %s
                - Category: %s
                - Description: %s
                """,
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getDescription()
        );
    }

    public static String summaryPrompt(Ticket ticket) {
        return buildTicketContext(ticket) + "\nPlease provide a concise summary of this ticket and its current state.";
    }

    public static String suggestedReplyPrompt(Ticket ticket) {
        return buildTicketContext(ticket) + "\nPlease draft a professional customer-facing reply for this ticket.";
    }

    private PromptTemplates() {}
}
