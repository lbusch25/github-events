package com.lbusch.github.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubEventPayload(
        @JsonProperty("repository_id") Long repositoryId,
        @JsonProperty("push_id") Long pushId,
        String ref,
        String head,
        String before
) {}
