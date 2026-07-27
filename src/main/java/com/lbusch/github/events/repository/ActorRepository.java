package com.lbusch.github.events.repository;

import com.lbusch.github.events.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActorRepository extends JpaRepository<Actor, UUID> {

    boolean existsByActorId(Long actorId);
}
