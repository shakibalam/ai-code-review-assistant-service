package com.flyct.prreview.client;

import com.flyct.prreview.dto.BitbucketCommentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Base64;

/**
 * BITBUCKET CLIENT: Handles all REST communication with Bitbucket API.
 * Includes retries, timeouts, and error handling.
 */
@Component
@Slf4j
public class BitbucketClient {

    private final WebClient webClient;
    private final String authHeader;
    private final int timeoutMs;

    public BitbucketClient(
            WebClient.Builder webClientBuilder,
            @Value("${bitbucket.base-url}") String baseUrl,
            @Value("${bitbucket.username}") String username,
            @Value("${bitbucket.app-password}") String password,
            @Value("${bitbucket.timeout-ms:5000}") int timeoutMs) {
        
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        this.timeoutMs = timeoutMs;
    }

    /**
     * FETCH DIFF: Includes 3 retries with exponential backoff (1s, 2s, 4s).
     */
    public Mono<String> fetchPrDiff(String workspace, String repoSlug, Long prId) {
        log.info("FETCHING DIFF: PR #{} for {}/{}", prId, workspace, repoSlug);
        
        return webClient.get()
                .uri("/repositories/{workspace}/{repo_slug}/pullrequests/{pull_request_id}/diff", 
                     workspace, repoSlug, prId)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> log.warn("RETRYING FETCH DIFF: Attempt #{}", signal.totalRetries() + 1)))
                .onErrorResume(e -> {
                    log.error("ERROR FETCHING DIFF after retries: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * POST COMMENT: Includes 3 retries with exponential backoff.
     */
    public Mono<Void> postComment(String workspace, String repoSlug, Long prId, String comment) {
        log.info("POSTING COMMENT: PR #{}", prId);
        
        BitbucketCommentRequest request = BitbucketCommentRequest.builder()
                .content(new BitbucketCommentRequest.Content(comment))
                .build();

        return webClient.post()
                .uri("/repositories/{workspace}/{repo_slug}/pullrequests/{pull_request_id}/comments", 
                     workspace, repoSlug, prId)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> log.warn("RETRYING POST COMMENT: Attempt #{}", signal.totalRetries() + 1)))
                .doOnSuccess(v -> log.info("POST SUCCESS: Commented on PR #{}", prId))
                .doOnError(e -> log.error("POST FAILED: Could not post comment to Bitbucket: {}", e.getMessage()));
    }
}