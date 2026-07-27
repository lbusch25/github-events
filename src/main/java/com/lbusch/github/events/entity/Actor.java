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
@Table(name = "actors")
@Getter
@Setter
@NoArgsConstructor
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "actor_id", nullable = false, unique = true)
    private Long actorId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "avatar_url", nullable = false)
    private String avatarUrl;

    @Column(name = "gravatar_id")
    private String gravatarId;

    @Column(name = "display_login")
    private String displayLogin;

    public Actor(Long actorId, String url, String login, String avatarUrl, String gravatarId, String displayLogin) {
        this.actorId = actorId;
        this.url = url;
        this.login = login;
        this.avatarUrl = avatarUrl;
        this.gravatarId = gravatarId;
        this.displayLogin = displayLogin;
    }
}
