package com.lbusch.github.events.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GitHubRestClient {

    private final String eventsUrl;

    public GitHubRestClient(@Value("${github.events.url}") String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }

    @Bean
    public RestClient gitHubEventsRestClient() {
        var restClientBuilder = RestClient.builder();
        return restClientBuilder
                .baseUrl(eventsUrl)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }    
}
