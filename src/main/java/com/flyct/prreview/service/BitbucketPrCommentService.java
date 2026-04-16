package com.flyct.prreview.service;

import com.flyct.prreview.client.BitbucketClient;
import com.flyct.prreview.dto.AiReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.stream.Collectors;

/**
 * COMMENT SERVICE: Formats and posts the review to Bitbucket.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BitbucketPrCommentService {

    private final BitbucketClient bitbucketClient;

    public Mono<Void> postReviewComment(String workspace, String repoSlug, Long prId, AiReviewResponse response) {
        log.info("POSTING REVIEW: PR #{} with {} suggestions", prId, 
                response.getSuggestions() != null ? response.getSuggestions().size() : 0);

        // 1. Post General Summary Comment
        String summaryComment = formatSummaryMarkdown(response);
        Mono<Void> summaryMono = bitbucketClient.postComment(workspace, repoSlug, prId, summaryComment);

        // 2. Post Inline Comments for each suggestion
        if (response.getSuggestions() != null && !response.getSuggestions().isEmpty()) {
            List<Mono<Void>> inlineCommentMonos = response.getSuggestions().stream()
                    .filter(s -> s.getFilePath() != null && s.getLineNumber() != null)
                    .map(s -> bitbucketClient.postInlineComment(
                            workspace, repoSlug, prId, 
                            "🤖 **AI Suggestion:** " + s.getComment(), 
                            s.getFilePath(), 
                            s.getLineNumber()))
                    .collect(Collectors.toList());

            return summaryMono.then(Mono.when(inlineCommentMonos));
        }

        return summaryMono;
    }
    private String formatSummaryMarkdown(AiReviewResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 AI PR Review Assistant\n\n");
        
        sb.append("**Summary**\n");
        sb.append(response.getSummary()).append("\n\n");
        
        sb.append("**Severity**\n");
        String severity = response.getSeverity().toUpperCase();
        String emoji;
        switch (severity) {
            case "CRITICAL":
                emoji = "🔴 ";
                break;
            case "HIGH":
                emoji = "🟠 ";
                break;
            case "MEDIUM":
                emoji = "🟡 ";
                break;
            default:
                emoji = "⚪ ";
                break;
        }
        sb.append(emoji).append("`").append(response.getSeverity()).append("`").append("\n\n");
        
        if (response.getSuggestions() == null || response.getSuggestions().isEmpty()) {
            sb.append("- *No major suggestions. Great work!*");
        } else {
            sb.append("Please check the inline comments for specific suggestions.");
        }
        
        sb.append("\n---\n*Disclaimer: AI-generated code review. Please verify before merging.*");
        return sb.toString();
    }
}