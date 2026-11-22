package dev.abhay.gitbrain.services;

import dev.abhay.gitbrain.Repository.RepositoryRepository;
import dev.abhay.gitbrain.Repository.UserRepository;
import dev.abhay.gitbrain.dto.RepositoryResponse;
import dev.abhay.gitbrain.dto.SyncSummary;
import dev.abhay.gitbrain.entity.Repository;
import dev.abhay.gitbrain.entity.RepositorySyncStatus;
import dev.abhay.gitbrain.entity.User;
import dev.abhay.gitbrain.integration.github.GithubClient;
import dev.abhay.gitbrain.integration.github.dto.GithubRepositoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final GithubClient githubApiClient;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final TextEncryptor tokenEncryptor;

    /** DB-only, paginated read — used by page load / pagination in the UI. */
    @Transactional(readOnly = true)
    public Page<RepositoryResponse> getRepos(UUID userId, Pageable pageable) {
        return repositoryRepository
                .findByUserId(userId, pageable)
                .map(RepositoryResponse::from);
    }

    /** Triggers a full GitHub fetch + DB upsert. Used by "Sync now". */
    @Transactional
    public SyncSummary syncRepositories(User user) {
        String accessToken = tokenEncryptor.decrypt(user.getAccessToken());
        List<GithubRepositoryResponse> githubRepos = githubApiClient.listUserRepos(accessToken);

        List<Long> existingIds = repositoryRepository.findGithubIdsByUserId(user.getId());
        Set<Long> existingIdSet = new HashSet<>(existingIds);

        List<Repository> toSave = new ArrayList<>(githubRepos.size());
        Set<Long> seenGithubIds = new HashSet<>(githubRepos.size());

        int addedCount = 0;
        int updatedCount = 0;

        for (GithubRepositoryResponse dto : githubRepos) {
            boolean isNew = !existingIdSet.contains(dto.getId());

            Repository repo = repositoryRepository
                    .findByUserIdAndGithubId(user.getId(), dto.getId())
                    .orElseGet(Repository::new);

            boolean pushedChanged = repo.getGithubPushedAt() != null
                    && !repo.getGithubPushedAt().equals(dto.getPushedAt());

            repo.setUser(user);
            repo.setGithubId(dto.getId());
            repo.setName(dto.getName());
            repo.setFullName(dto.getFullName());
            repo.setDescription(dto.getDescription());
            repo.setHtmlUrl(dto.getHtmlUrl());
            repo.setCloneUrl(dto.getCloneUrl());
            repo.setDefaultBranch(dto.getDefaultBranch());
            repo.setLanguage(dto.getLanguage());
            repo.setPrivateRepository(dto.isPrivateRepository());
            repo.setFork(dto.isFork());
            repo.setVisibility(dto.getVisibility());
            repo.setGithubCreatedAt(dto.getCreatedAt());
            repo.setGithubUpdatedAt(dto.getUpdatedAt());
            repo.setGithubPushedAt(dto.getPushedAt());

            RepositorySyncStatus syncStatus = repo.getSyncStatus();
            if (syncStatus == null) {
                syncStatus = new RepositorySyncStatus(null, repo, null, false, false, Instant.now());
                repo.setSyncStatus(syncStatus);
            }
            syncStatus.setHasUnsyncedChanges(isNew || pushedChanged);
            syncStatus.setLastSyncedAt(Instant.now());
            syncStatus.setStale(false);

            toSave.add(repo);
            seenGithubIds.add(dto.getId());

            if (isNew) {
                addedCount++;
            } else if (pushedChanged) {
                updatedCount++;
            }
        }

        repositoryRepository.saveAll(toSave);

        List<Long> missingIds = existingIds.stream()
                .filter(id -> !seenGithubIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            repositoryRepository.deleteByUserIdAndGithubIdIn(user.getId(), missingIds);
        }

        return SyncSummary.builder()
                .addedCount(addedCount)
                .updatedCount(updatedCount)
                .deletedCount(missingIds.size())
                .build();
    }
}