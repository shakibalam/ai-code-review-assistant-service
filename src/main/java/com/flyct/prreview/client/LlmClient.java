package com.flyct.prreview.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyct.prreview.dto.AiReviewResponse;
import com.flyct.prreview.dto.LlmRawResponse;
import com.flyct.prreview.dto.LlmRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

/**
 * LLM CLIENT: Communicates with OpenAI-compatible APIs.
 * Includes strict JSON parsing, retries, and fallback logic.
 */
@Component
@Slf4j
public class LlmClient {

    private final WebClient webClient;
    private final String model;
    private final ObjectMapper objectMapper;
    private final int timeoutMs;
    private final int maxRetries;

    public LlmClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${llm.openai.endpoint}") String endpoint,
            @Value("${llm.openai.api-key}") String apiKey,
            @Value("${llm.openai.model}") String model,
            @Value("${llm.openai.timeout-ms:30000}") int timeoutMs,
            @Value("${llm.openai.max-retries:3}") int maxRetries) {

        this.webClient = webClientBuilder
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
        this.objectMapper = objectMapper;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;

        if (apiKey == null || apiKey.equals("sk-placeholder") || apiKey.equals("your-key-here")) {
            log.error("LLM CONFIG ERROR: API Key is MISSING or using placeholder!");
        } else {
            String masked = apiKey.substring(0, Math.min(apiKey.length(), 8)) + "..." + apiKey.substring(Math.max(0, apiKey.length() - 4));
            log.info("LLM CLIENT READY: Using model {} with API Key: {}", model, masked);
        }
    }

    /**
     * REVIEW PR: Retries based on configuration with exponential backoff on failure.
     */
    public Mono<AiReviewResponse> reviewPullRequest(String prompt) {
        log.info("CALLING AI: Requesting review using model {}", model);
        long startTime = System.currentTimeMillis();

        LlmRequest request = LlmRequest.builder()
                .model(model)
                .messages(Collections.singletonList(new LlmRequest.Message("user", prompt)))
                .responseFormat(new LlmRequest.ResponseFormat("json_object"))
                .build();

        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatus::isError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("LLM API ERROR RESPONSE: {} - {}", response.statusCode(), body);
                                    return Mono.error(
                                            new RuntimeException(
                                                    "LLM API Error: " + response.statusCode() + " " + body
                                            )
                                    );
                                })
                )
                .bodyToMono(LlmRawResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(2))
                        .doBeforeRetry(signal -> log.warn("RETRYING AI: Attempt #{}", signal.totalRetries() + 1)))
                .map(this::parseResponseWithFallback)
                .doOnSuccess(res -> log.info("AI SUCCESS: Review generated in {}ms", System.currentTimeMillis() - startTime))
                .onErrorResume(e -> {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    log.error("AI CRITICAL FAILURE: LLM communication error: {} | Cause: {}", e.getMessage(), (cause != null ? cause.getMessage() : "Unknown"));

                    String errorMessage = "AI Review Service error: " + (cause != null ? cause.getMessage() : e.getMessage());

                    if (cause instanceof TimeoutException) {
                        errorMessage = "AI Review Service is temporarily unavailable due to timeout.";
                    } else if (cause != null && cause.getMessage() != null && cause.getMessage().contains("401")) {
                        errorMessage = "AI Review Service Authentication failed. Please verify LLM_API_KEY environment variable.";
                    } else if (e.getMessage() != null && e.getMessage().contains("Retries exhausted")) {
                        errorMessage = "AI Review Service failed after multiple attempts. " + (cause != null ? "Root cause: " + cause.getMessage() : "");
                    }

                    return Mono.just(createFallbackResponse(errorMessage));
                });
    }

    /**
     * STRICT PARSING with Fallback if JSON is malformed.
     */
    private AiReviewResponse parseResponseWithFallback(LlmRawResponse raw) {
        try {
            if (raw.getChoices() == null || raw.getChoices().isEmpty()) {
                throw new RuntimeException("EMPTY AI RESPONSE");
            }
            String content = raw.getChoices().get(0).getMessage().getContent();
            return objectMapper.readValue(content, AiReviewResponse.class);
        } catch (Exception e) {
            log.error("AI PARSE FAILURE: Malformed JSON output: {}", e.getMessage());
            return createFallbackResponse("AI response parsing failed. Please perform manual review.");
        }
    }

    private AiReviewResponse createFallbackResponse(String message) {
        return AiReviewResponse.builder()
                .summary("AI Review System Notification")
                .severity("LOW")
                .suggestions(Collections.singletonList(
                        AiReviewResponse.Suggestion.builder()
                                .comment(message)
                                .build()
                ))
                .build();
    }
}