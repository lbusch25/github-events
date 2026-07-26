package com.lbusch.github.events.repository;

import com.lbusch.github.events.entity.PushEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PushEventRepository extends JpaRepository<PushEvent, UUID> {

    boolean existsByEventId(String eventId);
}
