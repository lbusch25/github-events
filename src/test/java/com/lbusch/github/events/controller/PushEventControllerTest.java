package com.lbusch.github.events.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lbusch.github.events.entity.PushEvent;
import com.lbusch.github.events.service.PushEventService;

@WebMvcTest(PushEventController.class)
class PushEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PushEventService pushEventService;

    @Test
    void search_returnsEvents_whenMatchesExist() throws Exception {
        PushEvent event = new PushEvent("evt-1", 1L, 100L, 200L, "refs/heads/main", "abc123", "def456", "{\"type\":\"PushEvent\"}");
        when(pushEventService.search(any())).thenReturn(List.of(event));

        mockMvc.perform(get("/events")
                        .param("repositoryId", "100")
                        .param("ref", "refs/heads/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$[0].repositoryId").value(100))
                .andExpect(jsonPath("$[0].ref").value("refs/heads/main"));
    }

    @Test
    void search_returnsEmptyArray_whenNoMatches() throws Exception {
        when(pushEventService.search(any())).thenReturn(List.of());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_acceptsAllQueryParameters() throws Exception {
        when(pushEventService.search(any())).thenReturn(List.of());

        mockMvc.perform(get("/events")
                        .param("actorId", "1")
                        .param("repositoryId", "100")
                        .param("pushId", "200")
                        .param("ref", "refs/heads/main")
                        .param("head", "abc123")
                        .param("before", "def456"))
                .andExpect(status().isOk());
    }
}
