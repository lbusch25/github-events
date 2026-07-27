package com.lbusch.github.events.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbusch.github.events.dto.ActorData;
import com.lbusch.github.events.dto.GitHubEventResponse;
import com.lbusch.github.events.dto.RepoData;
import com.lbusch.github.events.entity.Actor;
import com.lbusch.github.events.entity.PushEvent;
import com.lbusch.github.events.entity.Repository;
import com.lbusch.github.events.repository.ActorRepository;
import com.lbusch.github.events.repository.PushEventRepository;
import com.lbusch.github.events.repository.RepositoryRepository;

@Service
public class GitHubEventsPoller {

    private static final Logger log = LoggerFactory.getLogger(GitHubEventsPoller.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PushEventRepository pushEventRepository;
    private final ActorRepository actorRepository;
    private final RepositoryRepository repositoryRepository;

    public GitHubEventsPoller(RestClient restClient,
                              ObjectMapper objectMapper,
                              PushEventRepository pushEventRepository,
                              ActorRepository actorRepository,
                              RepositoryRepository repositoryRepository) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.pushEventRepository = pushEventRepository;
        this.actorRepository = actorRepository;
        this.repositoryRepository = repositoryRepository;
    }

    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
    public void poll() {
        log.info("Polling GitHub Events API");

        String responseBody;
        try {
            responseBody = restClient.get()
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to fetch events from GitHub API: {}", e.getMessage());
            return;
        }

        if (responseBody == null || responseBody.isBlank()) {
            log.warn("Received empty response from GitHub API");
            return;
        }

        JsonNode nodes;
        try {
            nodes = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse GitHub API response: {}", e.getMessage());
            return;
        }

        int ingested = 0;
        int skipped = 0;

        for (JsonNode node : nodes) {
            GitHubEventResponse event;
            try {
                event = objectMapper.treeToValue(node, GitHubEventResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize event node: {}", e.getMessage());
                continue;
            }

            if (!"PushEvent".equals(event.type())) {
                continue;
            }

            if (event.id() == null) {
                log.warn("PushEvent missing 'id' field, skipping");
                continue;
            }

            if (pushEventRepository.existsByEventId(event.id())) {
                skipped++;
                continue;
            }

            persistActor(event.actor());
            persistRepository(event.repo());

            try {
                PushEvent pushEvent = new PushEvent(
                        event.id(),
                        event.actor().id(),
                        event.repo().id(),
                        event.payload().pushId(),
                        event.payload().ref(),
                        event.payload().head(),
                        event.payload().before(),
                        node.toString()
                );
                pushEventRepository.save(pushEvent);
                ingested++;
            } catch (DataIntegrityViolationException e) {
                skipped++;
            }
        }

        log.info("Poll complete: ingested={}, skipped(duplicate)={}", ingested, skipped);
    }

    private void persistActor(ActorData actorData) {
        if (actorData == null || actorData.id() == null) {
            return;
        }
        if (actorRepository.existsByActorId(actorData.id())) {
            return;
        }
        try {
            Actor actor = new Actor(
                    actorData.id(),
                    actorData.url(),
                    actorData.login(),
                    actorData.avatarUrl(),
                    actorData.gravatarId(),
                    actorData.displayLogin()
            );
            actorRepository.save(actor);
        } catch (DataIntegrityViolationException e) {
            // Already inserted by another thread/poll cycle
        }
    }

    private void persistRepository(RepoData repoData) {
        if (repoData == null || repoData.id() == null) {
            return;
        }
        if (repositoryRepository.existsByRepositoryId(repoData.id())) {
            return;
        }
        try {
            Repository repository = new Repository(
                    repoData.id(),
                    repoData.url(),
                    repoData.name()
            );
            repositoryRepository.save(repository);
        } catch (DataIntegrityViolationException e) {
            // Already inserted by another thread/poll cycle
        }
    }
}
