package dev.abhay.gitbrain.services;

import dev.abhay.gitbrain.Repository.RepositoryIndexStatusRepository;
import dev.abhay.gitbrain.entity.IndexingState;
import dev.abhay.gitbrain.entity.Repository;
import dev.abhay.gitbrain.entity.RepositoryIndexStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingWorker {

    private static final int CHUNK_SIZE       = 1_000;
    private static final int CHUNK_OVERLAP    = 100;
    private static final long MAX_FILE_BYTES  = 100_000;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java", ".kt", ".py", ".js", ".ts", ".tsx", ".jsx",
            ".go", ".rs", ".rb", ".php", ".c", ".cpp", ".h", ".cs",
            ".swift", ".md", ".txt", ".yml", ".yaml", ".json",
            ".xml", ".html", ".css", ".scss", ".sh", ".sql", ".toml",
            ".gradle", ".properties"
    );

    private static final Set<String> SKIP_DIRS = Set.of(
            ".git", "node_modules", "target", "build", "dist",
            ".idea", ".vscode", "__pycache__", ".gradle", "vendor"
    );

    private final RepositoryIndexStatusRepository indexStatusRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Async("indexingExecutor")
    public void doIndex(Repository repo, String decryptedAccessToken) {
        log.info("[Indexing] START repo={} ({})", repo.getId(), repo.getFullName());
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("gitbrain-clone-");
            cloneRepo(repo.getCloneUrl(), decryptedAccessToken, tempDir);
            log.info("[Indexing] Cloned to {}", tempDir);

            List<Document> documents = collectDocuments(repo, tempDir);
            log.info("[Indexing] Collected {} documents from {}", documents.size(), repo.getFullName());

            if (documents.isEmpty()) {
                markIndexed(repo, 0);
                return;
            }

            // Delete old chunks for this repository manually
            jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'repository_id' = ?", repo.getId().toString());

            // Spring AI handles batching automatically when adding documents
            vectorStore.add(documents);

            markIndexed(repo, documents.size());
            log.info("[Indexing] DONE repo={} — {} chunks", repo.getId(), documents.size());

        } catch (Exception ex) {
            log.error("[Indexing] FAILED repo={}: {}", repo.getId(), ex.getMessage(), ex);
            markFailed(repo, ex.getMessage());
        } finally {
            deleteTempDir(tempDir);
        }
    }

    private void cloneRepo(String cloneUrl, String accessToken, Path dir) throws Exception {
        Git.cloneRepository()
                .setURI(cloneUrl)
                .setDirectory(dir.toFile())
                .setDepth(1)
                .setNoCheckout(false)
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider("oauth2", accessToken))
                .call()
                .close();
    }

    private List<Document> collectDocuments(Repository repo, Path root) throws IOException {
        List<Document> result = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName() == null ? "" : dir.getFileName().toString();
                if (SKIP_DIRS.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                String ext      = extension(fileName);

                if (!ALLOWED_EXTENSIONS.contains(ext)) return FileVisitResult.CONTINUE;
                if (attrs.size() > MAX_FILE_BYTES)      return FileVisitResult.CONTINUE;

                try {
                    String content  = Files.readString(file);
                    String relPath  = root.relativize(file).toString().replace("\\", "/");
                    
                    result.addAll(chunk(repo, content, relPath));
                } catch (IOException e) {
                    log.warn("[Indexing] Could not read file {}: {}", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    private List<Document> chunk(Repository repo, String content, String filePath) {
        List<Document> chunks = new ArrayList<>();
        String prefix = "// File: " + filePath + "\n";
        int start = 0;
        int chunkIndex = 0;

        while (start < content.length()) {
            int end    = Math.min(start + CHUNK_SIZE, content.length());
            String raw = content.substring(start, end);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("repository_id", repo.getId().toString());
            metadata.put("file_path", filePath);
            metadata.put("chunk_index", chunkIndex++);
            
            chunks.add(new Document(prefix + raw, metadata));
            
            if (end == content.length()) break;
            start += (CHUNK_SIZE - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private void markIndexed(Repository repo, int chunkCount) {
        RepositoryIndexStatus status = indexStatusRepository.findByRepositoryId(repo.getId())
                .orElseGet(() -> new RepositoryIndexStatus(null, repo, IndexingState.NONE, null, null, Instant.now()));
        status.setStatus(IndexingState.INDEXED);
        status.setIndexedAt(Instant.now());
        status.setIndexError(null);
        indexStatusRepository.save(status);
    }

    private void markFailed(Repository repo, String error) {
        RepositoryIndexStatus status = indexStatusRepository.findByRepositoryId(repo.getId())
                .orElseGet(() -> new RepositoryIndexStatus(null, repo, IndexingState.NONE, null, null, Instant.now()));
        status.setStatus(IndexingState.FAILED);
        status.setIndexError(error != null && error.length() > 1000
                ? error.substring(0, 1000) : error);
        indexStatusRepository.save(status);
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot).toLowerCase() : "";
    }

    private void deleteTempDir(Path dir) {
        if (dir == null) return;
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                    Files.delete(f); return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                    Files.delete(d); return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("[Indexing] Could not delete temp dir {}: {}", dir, e.getMessage());
        }
    }
}
