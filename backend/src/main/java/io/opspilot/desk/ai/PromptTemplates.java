package io.opspilot.desk.ai;

public final class PromptTemplates {
    public static final String VERSION = "v1";

    public static final String SYSTEM_BASE =
        "You are an AI assistant for OpsPilot AI Desk, helping field operations support teams. " +
        "Be concise, professional, and focus on actionable advice.";

    public static final String SUMMARIZE_TICKET =
        "Summarize this support ticket in 2-3 sentences. Focus on the issue, current status, and any blockers.";

    public static final String SUGGEST_NEXT_ACTION =
        "Based on this ticket, suggest the most appropriate next action for the operator. " +
        "Be specific and actionable.";

    public static final String DRAFT_CUSTOMER_REPLY =
        "Draft a professional customer-facing reply for this ticket. " +
        "Be empathetic, clear, and include next steps if applicable.";

    public static final String IDENTIFY_MISSING_INFO =
        "Identify any missing information in this ticket that would help resolve it faster. " +
        "List each missing piece as a bullet point.";

    public static final String CLASSIFY_PRIORITY_CATEGORY =
        "Based on the ticket description, suggest the most appropriate priority (LOW/MEDIUM/HIGH/CRITICAL) " +
        "and category (DELIVERY/PICKUP/DOCUMENTATION/CUSTOMER/SYSTEM/OTHER). " +
        "Respond with: Priority: X, Category: Y, Reason: ...";

    private PromptTemplates() {}
}
