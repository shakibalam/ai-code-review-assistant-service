package com.flyct.prreview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * STRICT DTO: Matches the AI review JSON output structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResponse {
    private String summary;
    private String severity;
    private List<String> suggestions;
}