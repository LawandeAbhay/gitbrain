package dev.abhay.gitbrain.controllers;

import dev.abhay.gitbrain.dto.ApiResponse;
import dev.abhay.gitbrain.dto.ChatRequest;
import dev.abhay.gitbrain.dto.ChatResponse;
import dev.abhay.gitbrain.security.AppUserPrincipal;
import dev.abhay.gitbrain.security.CurrentUser;
import dev.abhay.gitbrain.services.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/repos")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUser currentUser;

    @PostMapping("/{id}/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @PathVariable UUID id,
            @Valid @RequestBody ChatRequest request
    ) {
        AppUserPrincipal principal = currentUser.require();

        ChatResponse response = chatService.chat(id, principal.getId(), request);

        return ResponseEntity.ok(ApiResponse.<ChatResponse>builder()
                .success(true)
                .message("Chat response generated")
                .data(response)
                .build());
    }
}