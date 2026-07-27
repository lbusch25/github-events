package com.lbusch.github.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepoData(
        Long id,
        String url,
        String name
) {}
