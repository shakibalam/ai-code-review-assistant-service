package com.flyct.prreview.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderServiceTest {

    private final PromptBuilderService promptBuilderService = new PromptBuilderService();

    @Test
    void testFilteringNonJavaFiles() {
        String diff = "diff --git a/README.md b/README.md\n+ New Readme\n" +
                      "diff --git a/src/Main.java b/src/Main.java\n+ public class Main {}";
        
        String prompt = promptBuilderService.buildPrompt(diff);
        
        assertFalse(prompt.contains("README.md"), "Should exclude non-java files");
        assertTrue(prompt.contains("Main.java"), "Should include java files");
    }

    @Test
    void testTruncation() {
        StringBuilder largeDiff = new StringBuilder();
        largeDiff.append("diff --git a/File1.java b/File1.java\n");
        for (int i = 0; i < 600; i++) {
            largeDiff.append("+ line ").append(i).append("\n");
        }
        
        String prompt = promptBuilderService.buildPrompt(largeDiff.toString());
        
        assertTrue(prompt.contains("truncated"), "Should contain truncation note");
    }
}