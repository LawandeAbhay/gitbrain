package dev.abhay.gitbrain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "message is required")
    @Size(max = 4000, message = "message must not exceed 4000 characters")
    private String message;

    @Valid
    private List<ChatMessageDto> history;

    public List<ChatMessageDto> getSafeHistory() {
        return history == null ? new ArrayList<>() : history;
    }
}