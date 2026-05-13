package com.opspilot.desk.ai;

import java.util.List;

public interface AiChatProvider {
    AiResponse chat(String systemPrompt, List<AiMessage> messages);
}
