package dev.abhay.gitbrain.Repository;

import dev.abhay.gitbrain.entity.Repository;
import dev.abhay.gitbrain.entity.RepositoryIndexStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositoryIndexStatusRepository extends JpaRepository<RepositoryIndexStatus, UUID> {

    Optional<RepositoryIndexStatus> findByRepository(Repository repository);

    /** Used inside the async worker where we only have the UUID (no entity ref). */
    Optional<RepositoryIndexStatus> findByRepositoryId(UUID repositoryId);
}
