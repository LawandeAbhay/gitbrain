package dev.abhay.gitbrain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "repository_sync_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_sync_status_repo",
                columnNames = "repository_id"
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositorySyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "repository_id",
            nullable = false
    )
    private Repository repository;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "has_unsynced_changes", nullable = false)
    private boolean hasUnsyncedChanges;

    @Column(nullable = false)
    private boolean stale;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = Instant.now();
    }
}
