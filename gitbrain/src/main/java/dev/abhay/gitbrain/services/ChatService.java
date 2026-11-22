package dev.abhay.gitbrain.services;

import dev.abhay.gitbrain.Repository.RepositoryRepository;
import dev.abhay.gitbrain.dto.ChatMessageDto;
import dev.abhay.gitbrain.dto.ChatRequest;
import dev.abhay.gitbrain.dto.ChatResponse;
import dev.abhay.gitbrain.entity.IndexingState;
import dev.abhay.gitbrain.entity.Repository;
import dev.abhay.gitbrain.entity.RepositoryIndexStatus;
import dev.abhay.gitbrain.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int TOP_K = 5;
    private static final int MAX_HISTORY_MESSAGES = 6;

    private final RepositoryRepository repositoryRepository;
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    @Transactional(readOnly = true)
    public ChatResponse chat(UUID repoId, UUID userId, ChatRequest request) {
        Repository repo = repositoryRepository.findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new UnauthorizedException("Repository not found or access denied"));

        RepositoryIndexStatus indexStatus = repo.getIndexStatus();
        if (indexStatus == null || indexStatus.getStatus() != IndexingState.INDEXED) {
            throw new IllegalStateException(
                    "Repository is not indexed yet. Current status: " +
                            (indexStatus != null ? indexStatus.getStatus() : "NONE"));
        }

        String question = request.getMessage();
        List<ChatMessageDto> history = request.getSafeHistory();

        // 1. Retrieve relevant chunks using VectorStore
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(TOP_K)
                        .filterExpression("repository_id == '" + repo.getId().toString() + "'")
                        .build()
        );

        // 2. Build context block
        StringBuilder contextBlock = new StringBuilder();
        List<String> sources = new ArrayList<>();
        for (Document doc : documents) {
            String filePath = (String) doc.getMetadata().get("file_path");
            contextBlock
                    .append("--- ")
                    .append(filePath)
                    .append(" ---\n")
                    .append(doc.getText())
                    .append("\n\n");
            if (filePath != null && !sources.contains(filePath)) {
                sources.add(filePath);
            }
        }

        // 3. Build system prompt
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate("""
                You are GitBrain, an expert AI code assistant specialized in analyzing GitHub repositories.
                You are currently helping a developer understand the repository: {repoName}.

                Answer questions clearly and concisely. Reference specific files when relevant.
                If the answer is not in the provided context, say so honestly rather than guessing.
                Format code snippets using markdown code blocks.

                === Relevant code context ===
                {context}
                === End of context ===
                """);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of(
                "repoName", repo.getFullName(),
                "context", contextBlock.toString()
        ));

        // 4. Assemble messages
        List<Message> messages = new ArrayList<>();
        messages.add(systemMessage);

        List<ChatMessageDto> trimmedHistory = history.size() > MAX_HISTORY_MESSAGES
                ? history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size())
                : history;

        for (ChatMessageDto msg : trimmedHistory) {
            if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            } else {
                messages.add(new UserMessage(msg.getContent()));
            }
        }

        messages.add(new UserMessage(question));

        // 5. Call LLM
        ChatClient chatClient = chatClientBuilder.build();
        String answer = chatClient.prompt(new Prompt(messages))
                .call()
                .content();

        log.info("[Chat] repo={} question='{}' sources={}",
                repoId, question.substring(0, Math.min(50, question.length())), sources);

        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setSources(sources);
        return response;
    }
}