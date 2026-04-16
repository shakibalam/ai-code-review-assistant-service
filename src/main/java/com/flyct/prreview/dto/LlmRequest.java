package com.flyct.prreview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {
    private String model;
    private List<Message> messages;
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseFormat {
        private String type;
    }
}