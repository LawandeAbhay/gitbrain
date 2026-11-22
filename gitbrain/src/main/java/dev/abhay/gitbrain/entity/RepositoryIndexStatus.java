package dev.abhay.gitbrain.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "repository_index_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_index_status_repo",
                columnNames = "repository_id"
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryIndexStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "repository_id",
            nullable = false
    )
    private Repository repository;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'NONE'")
    private IndexingState status = IndexingState.NONE;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Column(name = "index_error", columnDefinition = "TEXT")
    private String indexError;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = Instant.now();
        if (status == null) status = IndexingState.NONE;
    }
}
