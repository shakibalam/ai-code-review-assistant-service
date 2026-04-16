package com.flyct.prreview.service;

import com.flyct.prreview.client.BitbucketClient;
import com.flyct.prreview.dto.AiReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * COMMENT SERVICE: Formats and posts the review to Bitbucket.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BitbucketPrCommentService {

    private final BitbucketClient bitbucketClient;

    public Mono<Void> postReviewComment(String workspace, String repoSlug, Long prId, AiReviewResponse response) {
        log.info("PREPARING MARKDOWN: Formatting comment for PR #{}", prId);
        String formattedComment = formatMarkdown(response);
        return bitbucketClient.postComment(workspace, repoSlug, prId, formattedComment);
    }

    /**
     * CLEAN MARKDOWN: Upgraded for readability.
     */
    private String formatMarkdown(AiReviewResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 AI PR Review Assistant\n\n");
        
        sb.append("**Summary**\n");
        sb.append(response.getSummary()).append("\n\n");
        
        sb.append("**Severity**\n");
        sb.append("`").append(response.getSeverity()).append("`").append("\n\n");
        
        sb.append("**Suggestions**\n");
        if (response.getSuggestions() != null && !response.getSuggestions().isEmpty()) {
            response.getSuggestions().forEach(s -> sb.append("- ").append(s).append("\n"));
        } else {
            sb.append("- *No major suggestions. Great work!*");
        }
        
        sb.append("\n---\n*Disclaimer: AI-generated code review. Please verify before merging.*");
        return sb.toString();
    }
}