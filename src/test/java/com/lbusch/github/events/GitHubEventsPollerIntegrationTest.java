package com.lbusch.github.events;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.lbusch.github.events.repository.ActorRepository;
import com.lbusch.github.events.repository.PushEventRepository;
import com.lbusch.github.events.repository.RepositoryRepository;
import com.lbusch.github.events.service.GitHubEventsPoller;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GitHubEventsPollerIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private GitHubEventsPoller poller;

    @Autowired
    private PushEventRepository pushEventRepository;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("github.events.url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("spring.docker.compose.enabled", () -> "false");
    }

    @Test
    void poll_ingestsPushEvents_andPersistsActorAndRepository() {
        String eventsJson = """
                [
                    {
                        "id": "12345",
                        "type": "PushEvent",
                        "actor": {
                            "id": 583231,
                            "login": "octocat",
                            "display_login": "octocat",
                            "gravatar_id": "",
                            "url": "https://api.github.com/users/octocat",
                            "avatar_url": "https://avatars.githubusercontent.com/u/583231?v=4"
                        },
                        "repo": {
                            "id": 1296269,
                            "name": "octocat/Hello-World",
                            "url": "https://api.github.com/repos/octocat/Hello-World"
                        },
                        "payload": {
                            "repository_id": 1296269,
                            "push_id": 10115855396,
                            "ref": "refs/heads/master",
                            "head": "7a8f3ac80e2ad2f6842cb86f576d4bfe2c03e300",
                            "before": "883efe034920928c47fe18598c01249d1a9fdabd"
                        },
                        "public": true,
                        "created_at": "2022-06-09T12:47:28Z"
                    },
                    {
                        "id": "99999",
                        "type": "WatchEvent",
                        "actor": {
                            "id": 583231,
                            "login": "octocat",
                            "display_login": "octocat",
                            "gravatar_id": "",
                            "url": "https://api.github.com/users/octocat",
                            "avatar_url": "https://avatars.githubusercontent.com/u/583231?v=4"
                        },
                        "repo": {
                            "id": 1296269,
                            "name": "octocat/Hello-World",
                            "url": "https://api.github.com/repos/octocat/Hello-World"
                        },
                        "payload": {
                            "action": "started"
                        },
                        "public": true,
                        "created_at": "2022-06-09T12:47:28Z"
                    }
                ]
                """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(eventsJson)));

        poller.poll();

        // Verify push event was ingested
        assertThat(pushEventRepository.existsByEventId("12345")).isTrue();
        // Verify WatchEvent was filtered out
        assertThat(pushEventRepository.existsByEventId("99999")).isFalse();
        // Verify actor was persisted
        assertThat(actorRepository.existsByActorId(583231L)).isTrue();
        // Verify repository was persisted
        assertThat(repositoryRepository.existsByRepositoryId(1296269L)).isTrue();

        // Verify total count is exactly 1 push event
        assertThat(pushEventRepository.count()).isEqualTo(1);
    }

    @Test
    void poll_skipsDuplicateEvents() {
        String eventsJson = """
                [
                    {
                        "id": "67890",
                        "type": "PushEvent",
                        "actor": {
                            "id": 100,
                            "login": "testuser",
                            "display_login": "testuser",
                            "gravatar_id": "",
                            "url": "https://api.github.com/users/testuser",
                            "avatar_url": "https://avatars.githubusercontent.com/u/100?v=4"
                        },
                        "repo": {
                            "id": 200,
                            "name": "testuser/test-repo",
                            "url": "https://api.github.com/repos/testuser/test-repo"
                        },
                        "payload": {
                            "repository_id": 200,
                            "push_id": 555,
                            "ref": "refs/heads/feature",
                            "head": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                            "before": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                        },
                        "public": true,
                        "created_at": "2022-06-09T12:47:28Z"
                    }
                ]
                """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(eventsJson)));

        poller.poll();
        long countAfterFirst = pushEventRepository.count();

        // Poll again with same data
        poller.poll();
        long countAfterSecond = pushEventRepository.count();

        assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    }

    @Test
    void poll_handlesApiFailureGracefully() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        long countBefore = pushEventRepository.count();

        // Should not throw - handles gracefully
        poller.poll();

        assertThat(pushEventRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void poll_handlesMalformedJsonGracefully() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("not valid json at all")));

        long countBefore = pushEventRepository.count();

        // Should not throw
        poller.poll();

        assertThat(pushEventRepository.count()).isEqualTo(countBefore);
    }
}
