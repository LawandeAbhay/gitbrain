package dev.abhay.gitbrain.dto;

import dev.abhay.gitbrain.entity.Repository;
import dev.abhay.gitbrain.entity.RepositoryIndexStatus;
import dev.abhay.gitbrain.entity.RepositorySyncStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryResponse {
    // Core identity
    private UUID id;
    private Long githubId;
    private String name;
    private String fullName;
    private String description;
    private String htmlUrl;
    private String defaultBranch;
    private String language;
    private boolean privateRepository;
    private boolean fork;
    private String visibility;
    private Instant githubCreatedAt;
    private Instant githubUpdatedAt;
    private Instant githubPushedAt;

    // Sync status (from repository_sync_status)
    private Instant lastSyncedAt;
    private boolean hasUnsyncedChanges;

    // RAG index status (from repository_index_status)
    private String indexStatus;   // e.g. "NONE" | "INDEXING" | "INDEXED" | "FAILED"
    private Instant indexedAt;

    public static RepositoryResponse from(Repository repo) {
        RepositorySyncStatus  sync = repo.getSyncStatus();
        RepositoryIndexStatus idx  = repo.getIndexStatus();

        return new RepositoryResponse(
                repo.getId(),
                repo.getGithubId(),
                repo.getName(),
                repo.getFullName(),
                repo.getDescription(),
                repo.getHtmlUrl(),
                repo.getDefaultBranch(),
                repo.getLanguage(),
                repo.isPrivateRepository(),
                repo.isFork(),
                repo.getVisibility(),
                repo.getGithubCreatedAt(),
                repo.getGithubUpdatedAt(),
                repo.getGithubPushedAt(),
                // sync
                sync != null ? sync.getLastSyncedAt() : null,
                sync != null && sync.isHasUnsyncedChanges(),
                // index
                idx  != null ? idx.getStatus().name() : "NONE",
                idx  != null ? idx.getIndexedAt()     : null
        );
    }
}