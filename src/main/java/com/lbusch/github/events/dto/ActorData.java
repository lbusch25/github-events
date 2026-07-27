package com.lbusch.github.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActorData(
        Long id,
        String url,
        String login,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("gravatar_id") String gravatarId,
        @JsonProperty("display_login") String displayLogin
) {}
