package io.opspilot.desk.ai;

public record AiResponse(String content, int promptTokens, int completionTokens, String model, String errorMessage) {
    public boolean isError() {
        return errorMessage != null;
    }
}
