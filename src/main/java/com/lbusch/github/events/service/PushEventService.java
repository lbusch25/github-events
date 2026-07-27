package com.lbusch.github.events.service;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lbusch.github.events.dto.SearchPushEventRequest;
import com.lbusch.github.events.entity.PushEvent;
import com.lbusch.github.events.repository.PushEventRepository;
import com.lbusch.github.events.util.PushEventSpecificationUtils;

@Service
public class PushEventService {

    private final PushEventRepository pushEventRepository;

    public PushEventService(PushEventRepository pushEventRepository) {
        this.pushEventRepository = pushEventRepository;
    }

    public List<PushEvent> search(SearchPushEventRequest request) {
        Specification<PushEvent> spec = Specification
                .where(PushEventSpecificationUtils.hasActorId(request.actorId()))
                .and(PushEventSpecificationUtils.hasRepositoryId(request.repositoryId()))
                .and(PushEventSpecificationUtils.hasPushId(request.pushId()))
                .and(PushEventSpecificationUtils.hasRef(request.ref()))
                .and(PushEventSpecificationUtils.hasHead(request.head()))
                .and(PushEventSpecificationUtils.hasBefore(request.before()));

        return pushEventRepository.findAll(spec);
    }
}
