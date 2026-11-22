package dev.abhay.gitbrain.Repository;

import dev.abhay.gitbrain.entity.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    /**
     * Paginated repo list with sync + index status loaded in a single
     * LEFT JOIN — avoids N+1 queries on the dashboard.
     */
    @EntityGraph(attributePaths = {"syncStatus", "indexStatus"})
    Page<Repository> findByUserId(UUID userId, Pageable pageable);

    /**
     * Used by the sync service (per-repo upsert) — loads status relations
     * so the caller can update them in the same transaction.
     */
    @EntityGraph(attributePaths = {"syncStatus", "indexStatus"})
    Optional<Repository> findByUserIdAndGithubId(UUID userId, Long githubId);

    /**
     * Used by indexing + chat services — loads status relations so we
     * can verify ownership and check index state without extra queries.
     */
    @EntityGraph(attributePaths = {"syncStatus", "indexStatus"})
    Optional<Repository> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT r.githubId FROM Repository r WHERE r.user.id = :userId")
    List<Long> findGithubIdsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Repository r WHERE r.user.id = :userId AND r.githubId IN :githubIds")
    void deleteByUserIdAndGithubIdIn(@Param("userId") UUID userId,
                                     @Param("githubIds") List<Long> githubIds);
}