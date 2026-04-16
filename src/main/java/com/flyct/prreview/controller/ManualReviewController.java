package com.flyct.prreview.controller;

import com.flyct.prreview.dto.AiReviewResponse;
import com.flyct.prreview.dto.ManualReviewRequest;
import com.flyct.prreview.service.BitbucketPrCommentService;
import com.flyct.prreview.client.LlmClient;
import com.flyct.prreview.service.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * MANUAL FALLBACK: Allows demoing by pasting a diff content directly.
 */
@RestController
@RequestMapping("/manual")
@RequiredArgsConstructor
@Slf4j
public class ManualReviewController {

    private final LlmClient llmClient;
    private final PromptBuilderService promptBuilderService;
    private final BitbucketPrCommentService bitbucketPrCommentService;

    @Value("${bitbucket.workspace:demo-workspace}")
    private String workspace;

    @PostMapping("/review")
    public Mono<ResponseEntity<AiReviewResponse>> handleManualReview(@RequestBody ManualReviewRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        
        log.info("MANUAL FALLBACK: Reviewing PR #{} for repo {}", request.getPrId(), request.getRepo());

        // Step 1: Filter and Truncate logic is handled by PromptBuilder
        String prompt = promptBuilderService.buildPrompt(request.getDiff());
        
        // Step 2: Call LLM
        return llmClient.reviewPullRequest(prompt)
                .flatMap(reviewResponse -> {
                    // Optional: Post back to Bitbucket if prId and repo are provided
                    if (request.isPostComment() && request.getPrId() != null) {
                        return bitbucketPrCommentService.postReviewComment(
                                workspace, 
                                request.getRepo(), 
                                Long.parseLong(request.getPrId()), 
                                reviewResponse
                        ).thenReturn(ResponseEntity.ok(reviewResponse));
                    }
                    return Mono.just(ResponseEntity.ok(reviewResponse));
                });
    }
}