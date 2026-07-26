package com.lbusch.github.events.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lbusch.github.events.dto.SearchPushEventRequest;
import com.lbusch.github.events.entity.PushEvent;
import com.lbusch.github.events.service.PushEventService;

@RestController
@RequestMapping("/events")
public class PushEventController {

    private final PushEventService pushEventService;

    public PushEventController(PushEventService pushEventService) {
        this.pushEventService = pushEventService;
    }

    @GetMapping
    public List<PushEvent> search(SearchPushEventRequest request) {
        return pushEventService.search(request);
    }
}
