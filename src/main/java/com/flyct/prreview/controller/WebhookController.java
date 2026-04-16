package com.flyct.prreview.controller;

import com.flyct.prreview.dto.BitbucketWebhookEvent;
import com.flyct.prreview.service.ReviewCopilotCoreService;
import com.flyct.prreview.service.WebhookValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ENTRY POINT: Handles incoming webhooks from Bitbucket.
 * This class validates the request and immediately returns a 202 Accepted,
 * then triggers the AI review process asynchronously.
 */
@RestController
@RequestMapping("/webhook/bitbucket")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ReviewCopilotCoreService reviewCopilotCoreService;
    private final WebhookValidationService webhookValidationService;

    @PostMapping("/pr-review")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature,
            @RequestBody BitbucketWebhookEvent event) {
        
        // Step 1: Generate a unique ID for this request for log tracking (Distributed Tracing)
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        
        log.info("WEBHOOK RECEIVED: {} for PR #{}", 
                event.getEventKey(), 
                event.getPullrequest() != null ? event.getPullrequest().getId() : "unknown");

        // Step 2: Validate the secret token to ensure only Bitbucket can call this API
        if (!webhookValidationService.isValidSignature(signature)) {
            log.warn("VALIDATION FAILED: Invalid signature header");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Invalid Signature");
        }

        // Step 3: Trigger the review workflow (ASYNCHRONOUSLY)
        // This ensures Bitbucket doesn't timeout while waiting for the AI response.
        reviewCopilotCoreService.processReview(event);

        // Step 4: Acknowledge receipt to Bitbucket
        return ResponseEntity.accepted().body("Workflow started with Tracking ID: " + correlationId);
    }
}