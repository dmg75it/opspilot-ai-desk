package com.opspilot.desk.ai;

import com.opspilot.desk.entity.Ticket;

public final class PromptTemplates {

    public static final String SYSTEM_PROMPT = """
            You are OpsPilot, an AI assistant for field operations support teams.
            You help operators resolve tickets efficiently and professionally.
            Always be concise, actionable, and professional.
            """;

    public static String buildSummarizePrompt(Ticket ticket) {
        return String.format("""
                Please summarize the following support ticket concisely:

                Title: %s
                Status: %s
                Priority: %s
                Category: %s

                Description:
                %s

                Provide a brief summary of the issue and current state.
                """,
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getDescription());
    }

    public static String buildSuggestReplyPrompt(Ticket ticket) {
        return String.format("""
                Draft a professional customer-facing reply for the following support ticket:

                Title: %s
                Category: %s
                Priority: %s

                Description:
                %s

                Write a concise, empathetic reply that acknowledges the issue and sets expectations.
                """,
                ticket.getTitle(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getDescription());
    }

    public static String buildClassifyPrompt(Ticket ticket) {
        return String.format("""
                Analyze the following support ticket and suggest:
                1. The most appropriate priority (LOW, MEDIUM, HIGH, CRITICAL)
                2. The most appropriate category (DELIVERY, PICKUP, DOCUMENTATION, CUSTOMER, SYSTEM, OTHER)

                Title: %s

                Description:
                %s

                Respond with JSON: {"priority": "...", "category": "...", "reasoning": "..."}
                """,
                ticket.getTitle(),
                ticket.getDescription());
    }

    private PromptTemplates() {}
}
