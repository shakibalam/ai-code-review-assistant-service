package com.flyct.prreview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebhookValidationService {

    @Value("${bitbucket.webhook-secret}")
    private String webhookSecret;

    public boolean isValidSignature(String signature) {
        if (signature == null || !signature.contains(webhookSecret)) {
            log.warn("Webhook validation failed. Expected secret in signature: {}", webhookSecret);
            return false;
        }
        return true;
    }
}