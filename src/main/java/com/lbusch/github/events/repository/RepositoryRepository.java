package com.lbusch.github.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends JpaRepository<com.lbusch.github.events.entity.Repository, UUID> {

    boolean existsByRepositoryId(Long repositoryId);
}
