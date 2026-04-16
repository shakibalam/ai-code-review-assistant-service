package com.flyct.prreview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BitbucketCommentRequest {
    private Content content;
    private Inline inline;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String raw;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Inline {
        private String path;
        private Integer to;
        private Integer from;
    }
}