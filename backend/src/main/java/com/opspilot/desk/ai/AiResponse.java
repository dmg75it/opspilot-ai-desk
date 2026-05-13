package com.opspilot.desk.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiResponse {
    private String content;
    private String model;
    private Integer tokenCount;
}
