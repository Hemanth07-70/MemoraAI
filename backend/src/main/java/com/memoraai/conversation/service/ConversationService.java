package com.memoraai.conversation.service;

import com.memoraai.conversation.dto.ChatMessageDto;
import com.memoraai.conversation.dto.ConversationDto;
import com.memoraai.conversation.dto.CreateConversationRequest;
import com.memoraai.conversation.entity.ChatMessage;
import com.memoraai.conversation.entity.Conversation;
import com.memoraai.conversation.repository.ChatMessageRepository;
import com.memoraai.conversation.repository.ConversationRepository;
import com.memoraai.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ConversationDto createConversation(CreateConversationRequest request, User user) {
        Conversation conversation = Conversation.builder()
                .title(request.getTitle())
                .user(user)
                .build();
        Conversation saved = conversationRepository.save(conversation);
        log.info("Created conversation {} for user {}", saved.getId(), user.getId());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(UUID userId) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversation(UUID id, UUID userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found or access denied"));
        return mapToDto(conversation);
    }

    @Transactional
    public void deleteConversation(UUID id, UUID userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found or access denied"));
        conversationRepository.delete(conversation);
        log.info("Deleted conversation {}", id);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getConversationMessages(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found or access denied"));
        
        return chatMessageRepository.findAllByConversationIdOrderByTimestampAsc(conversation.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addMessageToConversation(UUID conversationId, UUID userId, com.memoraai.conversation.entity.MessageRole role, String content) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found or access denied"));

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();
        
        chatMessageRepository.save(message);
        
        // Update conversation timestamp
        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private ConversationDto mapToDto(Conversation conversation) {
        return ConversationDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private ChatMessageDto mapToDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .role(message.getRole())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }
}
