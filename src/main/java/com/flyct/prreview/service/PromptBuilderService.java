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
        sb.append("  \"summary\": \"Brief summary of the PR\",\n");
        sb.append("  \"severity\": \"LOW/MEDIUM/HIGH/CRITICAL\",\n");
        sb.append("  \"suggestions\": [\n");
        sb.append("    {\n");
        sb.append("      \"file_path\": \"src/main/java/com/example/MyFile.java\",\n");
        sb.append("      \"line_number\": 42,\n");
        sb.append("      \"comment\": \"Specific suggestion for this line\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("STRICT RULES:\n");
        sb.append("1. Extract the EXACT file path from the 'diff --git' header (use the path after 'b/').\n");
        sb.append("2. Extract the EXACT line number where the change occurs. The line number must be from the NEW (+) version.\n");
        sb.append("3. If you cannot identify a specific line/file, mention it in 'summary' and DO NOT add to 'suggestions'.\n");
        sb.append("4. Use JSON keys exactly: 'file_path', 'line_number', 'comment'.\n");
        sb.append("5. IMPORTANT: 90% of the 'comment' content MUST be contained within code blocks (```java ... ```). Keep explanatory text to an absolute minimum (one sentence max).\n");
        sb.append("6. ALWAYS include a 'Correct Code' block using markdown in the comment to show how to fix the issue.\n");
        sb.append("Example comment: 'Use Objects.requireNonNull for null safety:\n\n```java\nObjects.requireNonNull(user, \"User cannot be null\");\n```'\n\n");
        
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

        // Unescape double-backslash sequences introduced by JSON transit
        String sanitizedDiff = rawDiff.replace("\\\\", "\\")
                                      .replace("\\n", "\n")
                                      .replace("\\\"", "\"");

        String[] lines = sanitizedDiff.split("\n");
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