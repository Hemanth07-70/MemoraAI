package com.memoraai.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAskRequest {

    @NotBlank(message = "Question is required")
    @Schema(description = "The question to ask the RAG engine", example = "What programming languages does the candidate know?")
    private String question;

    @Schema(description = "Optional document ID to restrict the search to a specific document")
    private UUID documentId;

    @Schema(description = "Optional conversation ID to provide chat history context")
    private UUID conversationId;

    @Builder.Default
    @Schema(description = "Maximum number of chunks to retrieve for context", example = "5", defaultValue = "5")
    private Integer topK = 5;
}
