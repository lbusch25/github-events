package com.lbusch.github.events.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lbusch.github.events.entity.PushEvent;

@Repository
public interface PushEventRepository extends JpaRepository<PushEvent, UUID>, JpaSpecificationExecutor<PushEvent> {

    boolean existsByEventId(String eventId);
}
