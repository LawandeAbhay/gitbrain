package dev.abhay.gitbrain.controllers;

import dev.abhay.gitbrain.Repository.UserRepository;
import dev.abhay.gitbrain.dto.ApiResponse;
import dev.abhay.gitbrain.dto.RepositoryResponse;
import dev.abhay.gitbrain.dto.SyncSummary;
import dev.abhay.gitbrain.entity.User;
import dev.abhay.gitbrain.exceptions.UnauthorizedException;
import dev.abhay.gitbrain.security.AppUserPrincipal;
import dev.abhay.gitbrain.security.CurrentUser;
import dev.abhay.gitbrain.services.IndexingService;
import dev.abhay.gitbrain.services.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService       repoService;
    private final UserRepository    userRepository;
    private final CurrentUser       currentUser;
    private final IndexingService   indexingService;

    /** Paginated repo list — DB only, no GitHub calls. */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RepositoryResponse>>> getRepos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AppUserPrincipal principal = currentUser.require();
        Pageable pageable = PageRequest.of(page, size);
        Page<RepositoryResponse> repos = repoService.getRepos(principal.getId(), pageable);
        

        return ResponseEntity.ok(ApiResponse.<Page<RepositoryResponse>>builder()
                .success(true)
                .message("Repositories fetched")
                .data(repos)
                .build());
    }

    /** "Sync now" — full GitHub fetch + DB upsert/delete. */
    @GetMapping("/sync")
    public ResponseEntity<ApiResponse<SyncSummary>> syncRepos() {
        AppUserPrincipal principal = currentUser.require();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        SyncSummary summary = repoService.syncRepositories(user);

        return ResponseEntity.ok(ApiResponse.<SyncSummary>builder()
                .success(true)
                .message("Sync completed")
                .data(summary)
                .build());
    }

    @PostMapping("/{id}/index")
    public ResponseEntity<ApiResponse<Map<String, String>>> indexRepo(@PathVariable UUID id) {
        AppUserPrincipal principal = currentUser.require();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        boolean started = indexingService.triggerIndex(id, user);

        if (!started) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Indexing already in progress")
                            .build());
        }

        return ResponseEntity.accepted()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("Indexing started")
                        .data(Map.of("repoId", id.toString()))
                        .build());
    }

    /**
     * GET /api/repos/{id}/index-status
     * Poll this endpoint to check indexing progress.
     */
    @GetMapping("/{id}/index-status")
    public ResponseEntity<ApiResponse<IndexingService.IndexStatus>> indexStatus(@PathVariable UUID id) {
        AppUserPrincipal principal = currentUser.require();
        IndexingService.IndexStatus status = indexingService.getStatus(id, principal.getId());

        return ResponseEntity.ok(ApiResponse.<IndexingService.IndexStatus>builder()
                .success(true)
                .message("Index status fetched")
                .data(status)
                .build());
    }
}