package dev.abhay.gitbrain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private Long githubId;

    @Column(nullable = false, length = 100)
    private String githubUsername;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(length = 500)
    private String tokenScopes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
