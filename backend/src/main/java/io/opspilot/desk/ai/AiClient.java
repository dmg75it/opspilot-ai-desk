package io.opspilot.desk.ai;

import java.util.List;

public interface AiClient {

    AiResponse chat(List<AiMessage> messages);
}
