package com.opspilot.desk.integration;

public final class PromptTemplates {

    public static final String VERSION = "v1";

    public static final String SYSTEM_PROMPT =
        "You are OpsPilot AI, an intelligent assistant for field operations support teams. " +
        "You help operators manage tickets, communicate with customers, and resolve operational issues " +
        "in transport and logistics. Be concise, professional, and actionable. " +
        "Never make up information. If you don't know something, say so.";

    public static String summarizeTicket(String title, String description, String status, String priority) {
        return String.format(
            "[%s] Please provide a brief summary of this support ticket:\n\nTitle: %s\nStatus: %s\nPriority: %s\n\nDescription:\n%s",
            VERSION, title, status, priority, description
        );
    }

    public static String suggestNextAction(String title, String description, String status) {
        return String.format(
            "[%s] Based on this ticket, suggest the most appropriate next action for the operator:\n\nTitle: %s\nStatus: %s\n\nDescription:\n%s",
            VERSION, title, status, description
        );
    }

    public static String draftCustomerReply(String title, String description) {
        return String.format(
            "[%s] Draft a professional customer-facing reply for this support ticket:\n\nTitle: %s\n\nDescription:\n%s",
            VERSION, title, description
        );
    }

    public static String classifyTicket(String title, String description) {
        return String.format(
            "[%s] Classify the priority and category of this ticket. Priorities: LOW, MEDIUM, HIGH, CRITICAL. Categories: DELIVERY, PICKUP, DOCUMENTATION, CUSTOMER, SYSTEM, OTHER.\n\nTitle: %s\n\nDescription:\n%s",
            VERSION, title, description
        );
    }

    public static String identifyMissingInfo(String title, String description) {
        return String.format(
            "[%s] Identify what key information is missing from this support ticket that would help resolve it:\n\nTitle: %s\n\nDescription:\n%s",
            VERSION, title, description
        );
    }

    private PromptTemplates() {}
}
