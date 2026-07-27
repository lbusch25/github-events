package com.lbusch.github.events.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repositories")
@Getter
@Setter
@NoArgsConstructor
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "repository_id", nullable = false, unique = true)
    private Long repositoryId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", nullable = false)
    private String name;

    public Repository(Long repositoryId, String url, String name) {
        this.repositoryId = repositoryId;
        this.url = url;
        this.name = name;
    }
}
