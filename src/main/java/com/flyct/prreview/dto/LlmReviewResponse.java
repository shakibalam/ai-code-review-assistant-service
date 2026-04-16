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
public class LlmReviewResponse {
    private String summary;
    private String severity;
    private List<String> suggestions;
}