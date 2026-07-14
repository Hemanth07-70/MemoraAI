package com.memoraai.chat.controller;

import com.memoraai.chat.dto.ChatAskRequest;
import com.memoraai.chat.dto.ChatAskResponse;
import com.memoraai.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "RAG Question Answering APIs")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    @Operation(summary = "Ask a question", description = "Uses Retrieval-Augmented Generation to answer a user's question based on indexed documents.")
    public ResponseEntity<ChatAskResponse> ask(
            @Valid @RequestBody ChatAskRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.memoraai.user.entity.User user) {
        ChatAskResponse response = chatService.askQuestion(request, user);
        return ResponseEntity.ok(response);
    }
}
