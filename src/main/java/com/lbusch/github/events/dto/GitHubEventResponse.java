package com.lbusch.github.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubEventResponse(
        String id,
        String type,
        GitHubEventPayload payload,
        ActorData actor,
        RepoData repo
) {}
