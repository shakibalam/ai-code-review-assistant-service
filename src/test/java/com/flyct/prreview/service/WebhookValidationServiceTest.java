package com.flyct.prreview.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "bitbucket.webhook-secret=test-secret")
class WebhookValidationServiceTest {

    @Autowired
    private WebhookValidationService validationService;

    @Test
    void whenSignatureMatches_thenValid() {
        assertTrue(validationService.isValidSignature("sha256=test-secret"));
    }

    @Test
    void whenSignatureMismatch_thenInvalid() {
        assertFalse(validationService.isValidSignature("wrong"));
    }

    @Test
    void whenSignatureNull_thenInvalid() {
        assertFalse(validationService.isValidSignature(null));
    }
}