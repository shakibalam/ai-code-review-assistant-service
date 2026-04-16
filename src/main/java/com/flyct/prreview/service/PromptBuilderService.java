package com.flyct.prreview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PROMPT BUILDER: Responsible for creating the instruction set for the AI.
 * Includes filtering and truncation logic for token safety.
 */
@Service
@Slf4j
public class PromptBuilderService {

    private static final int MAX_JAVA_FILES = 3;
    private static final int MAX_TOTAL_CHARS = 12000;
    private static final int MAX_LINES_PER_FILE = 500;

    /**
     * Builds a structured prompt from a raw diff patch.
     */
    public String buildPrompt(String rawDiff) {
        log.info("BUILDING PROMPT: Filtering and truncating PR diff");
        
        String processedDiff = processDiff(rawDiff);
        
        StringBuilder sb = new StringBuilder();
        sb.append("You are a Senior Java Architect and Staff Engineer.\n");
        sb.append("Review the following PR diff and provide a structured review in JSON format.\n");
        sb.append("Focus on: Null safety, SOLID principles, Naming, Performance (Streams), code optimization, and Concurrency.\n\n");
        
        if (processedDiff.contains("NOTE: PR diff truncated for token safety")) {
            sb.append("NOTE: PR diff truncated for token safety. Please focus on the most important changes.\n\n");
        }

        sb.append("Output JSON schema:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"Brief summary\",\n");
        sb.append("  \"severity\": \"LOW/MEDIUM/HIGH/CRITICAL\",\n");
        sb.append("  \"suggestions\": [\"Suggestion 1\", \"Suggestion 2\"]\n");
        sb.append("}\n\n");
        
        sb.append("PR Diff Content:\n");
        sb.append("```diff\n");
        sb.append(processedDiff);
        sb.append("\n```");
        
        return sb.toString();
    }

    /**
     * Filters for .java files and truncates the content for safety.
     */
    private String processDiff(String rawDiff) {
        if (rawDiff == null || rawDiff.isEmpty()) return "";

        String[] lines = rawDiff.split("\n");
        StringBuilder result = new StringBuilder();
        int javaFileCount = 0;
        boolean inJavaFile = false;
        int fileLines = 0;
        boolean truncated = false;

        for (String line : lines) {
            // New file header detect
            if (line.startsWith("diff --git")) {
                if (line.endsWith(".java")) {
                    if (javaFileCount >= MAX_JAVA_FILES) {
                        truncated = true;
                        break;
                    }
                    inJavaFile = true;
                    javaFileCount++;
                    fileLines = 0;
                } else {
                    inJavaFile = false;
                }
            }

            if (inJavaFile) {
                if (fileLines < MAX_LINES_PER_FILE) {
                    result.append(line).append("\n");
                    fileLines++;
                } else {
                    truncated = true;
                }
            }

            if (result.length() > MAX_TOTAL_CHARS) {
                truncated = true;
                break;
            }
        }

        if (truncated) {
            result.append("\nNOTE: PR diff truncated for token safety\n");
        }

        return result.toString();
    }
}