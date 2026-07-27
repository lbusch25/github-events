package com.lbusch.github.events.service;

import com.lbusch.github.events.dto.SearchPushEventRequest;
import com.lbusch.github.events.entity.PushEvent;
import com.lbusch.github.events.repository.PushEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushEventServiceTest {

    @Mock
    private PushEventRepository pushEventRepository;

    @InjectMocks
    private PushEventService pushEventService;

    @Test
    void search_delegatesToRepository_withSpecification() {
        PushEvent event = new PushEvent("event-1", 1L, 100L, 200L, "refs/heads/main", "abc", "def", "{}");
        when(pushEventRepository.findAll(any(Specification.class))).thenReturn(List.of(event));

        SearchPushEventRequest request = new SearchPushEventRequest(1L, 100L, 200L, "refs/heads/main", "abc", "def");
        List<PushEvent> results = pushEventService.search(request);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getEventId()).isEqualTo("event-1");
        verify(pushEventRepository).findAll(any(Specification.class));
    }

    @Test
    void search_returnsEmptyList_whenNoMatches() {
        when(pushEventRepository.findAll(any(Specification.class))).thenReturn(List.of());

        SearchPushEventRequest request = new SearchPushEventRequest(null, null, null, null, null, null);
        List<PushEvent> results = pushEventService.search(request);

        assertThat(results).isEmpty();
    }

    @Test
    void search_withPartialParameters_delegatesToRepository() {
        PushEvent event = new PushEvent("event-2", 2L, 101L, 201L, "refs/heads/feature", "xyz", "uvw", "{}");
        when(pushEventRepository.findAll(any(Specification.class))).thenReturn(List.of(event));

        SearchPushEventRequest request = new SearchPushEventRequest(null, 101L, null, null, null, null);
        List<PushEvent> results = pushEventService.search(request);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getRepositoryId()).isEqualTo(101L);
    }
}
