package com.flyct.prreview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRawResponse {
    private List<Choice> choices;
    private Usage usage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Message message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private int totalTokens;
    }
}