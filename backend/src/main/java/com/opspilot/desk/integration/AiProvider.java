package com.opspilot.desk.integration;

import java.util.List;

public interface AiProvider {
    AiResponse chat(String systemPrompt, List<AiMessage> messages);
    String getModelName();
}
