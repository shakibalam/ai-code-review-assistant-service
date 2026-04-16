package com.flyct.prreview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitbucketCommentRequest {
    private Content content;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String raw;
    }
}