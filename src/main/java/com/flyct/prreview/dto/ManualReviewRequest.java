package com.flyct.prreview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for manual PR diff submission during demos or as a fallback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewRequest {
    private String repo;
    private String prId;
    private String diff;
    private boolean postComment; // Optional: whether to post to Bitbucket or just return JSON
}