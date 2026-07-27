package com.lbusch.github.events.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "push_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "push_id", nullable = false)
    private Long pushId;

    @Column(name = "ref", nullable = false)
    private String ref;

    @Column(name = "head", nullable = false, length = 40)
    private String head;

    @Column(name = "before", nullable = false, length = 40)
    private String before;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    public PushEvent(String eventId, Long actorId, Long repositoryId, Long pushId, String ref, String head, String before, String rawPayload) {
        this.eventId = eventId;
        this.actorId = actorId;
        this.repositoryId = repositoryId;
        this.pushId = pushId;
        this.ref = ref;
        this.head = head;
        this.before = before;
        this.rawPayload = rawPayload;
    }
}
