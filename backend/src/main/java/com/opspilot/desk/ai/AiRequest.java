package com.opspilot.desk.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiRequest {
    private String model;
    private List<AiMessage> messages;
    private int maxTokens;
    private double temperature;
}
