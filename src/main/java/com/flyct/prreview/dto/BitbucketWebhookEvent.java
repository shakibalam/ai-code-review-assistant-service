package com.flyct.prreview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitbucketWebhookEvent {
    private String eventKey;
    private PullRequest pullrequest;
    private Repository repository;
    private Actor actor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PullRequest {
        private Long id;
        private String title;
        private String description;
        private String state;
        private Author author;
        @JsonProperty("source")
        private BranchInfo source;
        @JsonProperty("destination")
        private BranchInfo destination;
        private Links links;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        private String displayName;
        private String nickname;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInfo {
        private Branch branch;
        private Commit commit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Branch {
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Commit {
        private String hash;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Repository {
        private String name;
        @JsonProperty("full_name")
        private String fullName;
        private String slug;
        private Workspace workspace;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Workspace {
        private String slug;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Actor {
        private String displayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Links {
        private Link diff;
        private Link self;
        private Link comments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        private String href;
    }
}