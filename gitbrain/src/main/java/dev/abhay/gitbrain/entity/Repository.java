package dev.abhay.gitbrain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "repositories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_github_repo",
                        columnNames = {"user_id", "github_id"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "github_id", nullable = false)
    private Long githubId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "full_name", nullable = false, length = 500)
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "html_url", length = 500)
    private String htmlUrl;

    @Column(name = "clone_url", length = 500)
    private String cloneUrl;

    @Column(name = "default_branch", length = 255)
    private String defaultBranch;

    @Column(length = 100)
    private String language;

    @Column(nullable = false)
    private boolean privateRepository;

    @Column(nullable = false)
    private boolean fork;

    @Column(length = 50)
    private String visibility;

    @Column(name = "github_created_at")
    private Instant githubCreatedAt;

    @Column(name = "github_updated_at")
    private Instant githubUpdatedAt;

    @Column(name = "github_pushed_at")
    private Instant githubPushedAt;

    @OneToOne(mappedBy = "repository", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, orphanRemoval = true)
    private RepositorySyncStatus syncStatus;

    @OneToOne(mappedBy = "repository", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, orphanRemoval = true)
    private RepositoryIndexStatus indexStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}