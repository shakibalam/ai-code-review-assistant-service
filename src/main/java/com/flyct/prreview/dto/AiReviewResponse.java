package com.flyct.prreview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * STRICT DTO: Matches the AI review JSON output structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResponse {
    private String summary;
    private String severity;
    private List<Suggestion> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        @JsonProperty("file_path")
        private String filePath;

        @JsonProperty("line_number")
        private Integer lineNumber;

        private String comment;

        // Fallback for camelCase if LLM sends it
        @JsonProperty("filePath")
        public void setFilePathAlternative(String filePath) {
            this.filePath = filePath;
        }

        @JsonProperty("lineNumber")
        public void setLineNumberAlternative(Integer lineNumber) {
            this.lineNumber = lineNumber;
        }
    }
}