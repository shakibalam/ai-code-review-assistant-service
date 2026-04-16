package com.flyct.prreview.service;

import com.flyct.prreview.client.BitbucketClient;
import com.flyct.prreview.client.LlmClient;
import com.flyct.prreview.dto.BitbucketWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CORE ORCHESTRATOR: Coordinates the PR Review lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewCopilotCoreService {

    private final BitbucketClient bitbucketClient;
    private final LlmClient llmClient;
    private final PromptBuilderService promptBuilderService;
    private final BitbucketPrCommentService bitbucketPrCommentService;
    
    private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    @Async
    public void processReview(BitbucketWebhookEvent event) {
        String eventId = generateEventId(event);
        
        if (processedEvents.containsKey(eventId)) {
            log.info("SKIP DUPLICATE: PR #{} change already processed.", event.getPullrequest().getId());
            return;
        }

        try {
            processedEvents.put(eventId, true);
            log.info("PROCESS START: Reviewing PR #{}", event.getPullrequest().getId());

            String workspace = event.getRepository().getWorkspace().getSlug();
            String repoSlug = event.getRepository().getSlug();
            Long prId = event.getPullrequest().getId();

            bitbucketClient.fetchPrDiff(workspace, repoSlug, prId)
                    .flatMap(diff -> {
                        if (diff == null || diff.isEmpty()) {
                            log.warn("EMPTY DIFF: No changes detected for PR #{}", prId);
                            return Mono.empty();
                        }
                        log.info("DIFF FETCHED: Successfully retrieved PR patch.");
                        
                        String prompt = promptBuilderService.buildPrompt(diff);
                        log.info("PROMPT BUILT: Instructions generated for AI.");
                        
                        return llmClient.reviewPullRequest(prompt);
                    })
                    .flatMap(reviewResponse -> {
                        log.info("AI RESPONSE RECEIVED: Severity level: {}", reviewResponse.getSeverity());
                        return bitbucketPrCommentService.postReviewComment(workspace, repoSlug, prId, reviewResponse);
                    })
                    .doOnSuccess(v -> log.info("PROCESS COMPLETE: AI Review posted for PR #{}", prId))
                    .doOnError(e -> {
                        log.error("PROCESS FAILED: Unexpected error in workflow: {}", e.getMessage());
                        processedEvents.remove(eventId);
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to initiate PR review: {}", e.getMessage());
            processedEvents.remove(eventId);
        }
    }

    private String generateEventId(BitbucketWebhookEvent event) {
        return String.format("%s:%d:%s", 
                event.getRepository().getSlug(), 
                event.getPullrequest().getId(), 
                event.getPullrequest().getSource().getCommit().getHash());
    }
}