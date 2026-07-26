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
import com.lbusch.github.events.entity.PushEvent;
import com.lbusch.github.events.repository.PushEventRepository;

@Service
public class GitHubEventsPoller {

    private static final Logger log = LoggerFactory.getLogger(GitHubEventsPoller.class);

    private final RestClient restClient;
    private final PushEventRepository pushEventRepository;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public GitHubEventsPoller(RestClient restClient,
                              PushEventRepository pushEventRepository) {
        this.restClient = restClient;
        this.pushEventRepository = pushEventRepository;
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

        JsonNode events;
        try {
            events = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse GitHub API response: {}", e.getMessage());
            return;
        }

        int ingested = 0;
        int skipped = 0;

        for (JsonNode event : events) {
            String type = event.path("type").asText("");
            if (!"PushEvent".equals(type)) {
                continue;
            }

            String eventId = event.path("id").asText(null);
            if (eventId == null) {
                log.warn("PushEvent missing 'id' field, skipping");
                continue;
            }

            if (pushEventRepository.existsByEventId(eventId)) {
                skipped++;
                continue;
            }

            try {
                PushEvent pushEvent = new PushEvent(eventId, objectMapper.writeValueAsString(event));
                pushEventRepository.save(pushEvent);
                ingested++;
            } catch (DataIntegrityViolationException e) {
                // Race condition on unique constraint — another thread/restart already inserted it
                skipped++;
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize event {}: {}", eventId, e.getMessage());
            }
        }

        log.info("Poll complete: ingested={}, skipped(duplicate)={}", ingested, skipped);
    }
}
