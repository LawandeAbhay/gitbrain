package dev.abhay.gitbrain.Repository;

import dev.abhay.gitbrain.entity.Repository;
import dev.abhay.gitbrain.entity.RepositorySyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositorySyncStatusRepository extends JpaRepository<RepositorySyncStatus, UUID> {

    Optional<RepositorySyncStatus> findByRepository(Repository repository);

    Optional<RepositorySyncStatus> findByRepositoryId(UUID repositoryId);
}
