package com.lbusch.github.events.dto;

public record SearchPushEventRequest(
        Long repositoryId,
        Long pushId,
        String ref,
        String head,
        String before
) {}
