package com.opspilot.desk.integration;

public record AiResponse(String content, String model, int tokenEstimate, boolean error, String errorMessage) {
    public static AiResponse success(String content, String model, int tokens) {
        return new AiResponse(content, model, tokens, false, null);
    }
    public static AiResponse failure(String errorMessage, String model) {
        return new AiResponse(null, model, 0, true, errorMessage);
    }
}
