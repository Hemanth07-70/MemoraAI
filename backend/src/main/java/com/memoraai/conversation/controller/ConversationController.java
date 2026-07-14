package com.memoraai.conversation.controller;

import com.memoraai.common.response.ApiResponse;
import com.memoraai.conversation.dto.ChatMessageDto;
import com.memoraai.conversation.dto.ConversationDto;
import com.memoraai.conversation.dto.CreateConversationRequest;
import com.memoraai.conversation.service.ConversationService;
import com.memoraai.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversation", description = "Endpoints for managing conversations and chat history")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "Create a new conversation", description = "Creates a new conversation for the authenticated user")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal User user) {
        ConversationDto dto = conversationService.createConversation(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation created successfully", dto));
    }

    @GetMapping
    @Operation(summary = "Get user conversations", description = "Retrieves all conversations for the authenticated user")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getConversations(
            @AuthenticationPrincipal User user) {
        List<ConversationDto> conversations = conversationService.getUserConversations(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID", description = "Retrieves a specific conversation")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        ConversationDto dto = conversationService.getConversation(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved successfully", dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a conversation", description = "Deletes a specific conversation and all its messages")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        conversationService.deleteConversation(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get conversation messages", description = "Retrieves all messages for a specific conversation")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getConversationMessages(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        List<ChatMessageDto> messages = conversationService.getConversationMessages(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }
}
