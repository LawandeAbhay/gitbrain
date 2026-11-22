package dev.abhay.gitbrain.services;

import dev.abhay.gitbrain.Repository.RepositoryIndexStatusRepository;
import dev.abhay.gitbrain.Repository.RepositoryRepository;
import dev.abhay.gitbrain.entity.*;
import dev.abhay.gitbrain.exceptions.ServerBusyException;
import dev.abhay.gitbrain.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryIndexStatusRepository indexStatusRepository;
    private final IndexingWorker indexingWorker;
    private final TextEncryptor tokenEncryptor;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public boolean triggerIndex(UUID repoId, User user) {
        Repository repo = repositoryRepository.findByIdAndUserId(repoId, user.getId())
                .orElseThrow(() -> new UnauthorizedException("Repository not found or access denied"));

        RepositoryIndexStatus indexStatus = repo.getIndexStatus();
        if (indexStatus == null) {
            indexStatus = new RepositoryIndexStatus();

            indexStatus.setRepository(repo);
            indexStatus.setStatus(IndexingState.NONE);
            indexStatus.setIndexedAt(null);
            indexStatus.setIndexError(null);
            repo.setIndexStatus(indexStatus);
        }

        if (indexStatus.getStatus() == IndexingState.INDEXING) {
            log.warn("Indexing already in progress for repo {}", repoId);
            return false;
        }

        indexStatus.setStatus(IndexingState.INDEXING);
        indexStatus.setIndexError(null);
        indexStatusRepository.save(indexStatus);

        String plainToken = tokenEncryptor.decrypt(user.getAccessToken());

        try {
            indexingWorker.doIndex(repo, plainToken);
        } catch (TaskRejectedException e) {
            throw new ServerBusyException("Server is busy please retry shortly");
        }
        return true;
    }

    @Transactional(readOnly = true)
    public IndexStatus getStatus(UUID repoId, UUID userId) {
        Repository repo = repositoryRepository.findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new UnauthorizedException("Repository not found or access denied"));

        RepositoryIndexStatus indexStatus = repo.getIndexStatus();
        IndexingState status = indexStatus != null ? indexStatus.getStatus() : IndexingState.NONE;
        
        int chunkCount = 0;
        if (status == IndexingState.INDEXED) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM vector_store WHERE metadata->>'repository_id' = ?", 
                    Integer.class, 
                    repoId.toString()
                );
                chunkCount = count != null ? count : 0;
            } catch (Exception e) {
                log.warn("Could not query chunk count from vector_store", e);
            }
        }

        return new IndexStatus(
                status.name(),
                indexStatus != null ? indexStatus.getIndexedAt() : null,
                chunkCount,
                indexStatus != null ? indexStatus.getIndexError() : null
        );
    }

    public record IndexStatus(
            String status,
            java.time.Instant indexedAt,
            int chunkCount,
            String error
    ) {}
}
