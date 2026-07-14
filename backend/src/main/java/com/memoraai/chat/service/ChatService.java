package com.memoraai.chat.service;

import com.memoraai.chat.dto.ChatAskRequest;
import com.memoraai.chat.dto.ChatAskResponse;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.search.dto.SearchResultItem;
import com.memoraai.search.service.CosineSimilarityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ChatService {

    private final EmbeddingService embeddingService;
    private final CosineSimilarityService cosineSimilarityService;
    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;
    private final com.memoraai.conversation.service.ConversationService conversationService;
    private final double hallucinationThreshold;

    public ChatService(EmbeddingService embeddingService,
                       CosineSimilarityService cosineSimilarityService,
                       PromptBuilderService promptBuilderService,
                       LLMService llmService,
                       com.memoraai.conversation.service.ConversationService conversationService,
                       @Value("${memoraai.ai.hallucination-threshold:0.30}") double hallucinationThreshold) {
        this.embeddingService = embeddingService;
        this.cosineSimilarityService = cosineSimilarityService;
        this.promptBuilderService = promptBuilderService;
        this.llmService = llmService;
        this.conversationService = conversationService;
        this.hallucinationThreshold = hallucinationThreshold;
    }

    public ChatAskResponse askQuestion(ChatAskRequest request, com.memoraai.user.entity.User user) {
        long startTime = System.currentTimeMillis();
        log.info("Question received: '{}'", request.getQuestion());
        
        List<com.memoraai.conversation.dto.ChatMessageDto> history = null;
        if (request.getConversationId() != null) {
            log.info("Fetching conversation history for conversation ID: {}", request.getConversationId());
            history = conversationService.getConversationMessages(request.getConversationId(), user.getId());
            conversationService.addMessageToConversation(request.getConversationId(), user.getId(), com.memoraai.conversation.entity.MessageRole.USER, request.getQuestion());
        }

        log.info("Generating query embedding...");

        List<Float> queryEmbedding = embeddingService.generateQueryEmbedding(request.getQuestion());

        log.info("Searching semantic index...");
        List<SearchResultItem> retrievedChunks = cosineSimilarityService.findSimilarChunks(
                queryEmbedding,
                request.getTopK() != null ? request.getTopK() : 5,
                request.getDocumentId(),
                user.getId()
        );

        long retrievalTimeMs = System.currentTimeMillis() - startTime;
        log.info("Retrieved {} chunks", retrievedChunks.size());

        if (retrievedChunks.isEmpty() || retrievedChunks.get(0).getScore() < hallucinationThreshold) {
            log.info("Highest chunk score is below threshold ({}), preventing hallucination", hallucinationThreshold);
            return ChatAskResponse.builder()
                    .answer("I couldn't find this information in the uploaded documents.")
                    .sources(Collections.emptyList())
                    .retrievalTimeMs(retrievalTimeMs)
                    .generationTimeMs(0)
                    .totalTimeMs(retrievalTimeMs)
                    .build();
        }

        log.info("Building prompt...");
        String prompt = promptBuilderService.buildPrompt(request.getQuestion(), retrievedChunks, history);

        log.info("Calling LLM Provider: {}, Model: {}", llmService.getProviderName(), llmService.getModelName());
        long generationStartTime = System.currentTimeMillis();
        
        String answer;
        try {
            answer = llmService.generateAnswer(prompt).block();
        } catch (Exception e) {
            log.error("Failed to generate answer", e);
            throw new RuntimeException("Failed to generate answer from LLM: " + e.getMessage(), e);
        }

        long generationTimeMs = System.currentTimeMillis() - generationStartTime;
        long totalTimeMs = System.currentTimeMillis() - startTime;

        if (request.getConversationId() != null) {
            conversationService.addMessageToConversation(request.getConversationId(), user.getId(), com.memoraai.conversation.entity.MessageRole.AI, answer);
        }

        log.info("LLM generation completed in {} ms", generationTimeMs);
        log.info("Returning response. Total request time {} ms", totalTimeMs);

        return ChatAskResponse.builder()
                .answer(answer)
                .provider(llmService.getProviderName())
                .model(llmService.getModelName())
                .sources(retrievedChunks)
                .retrievalTimeMs(retrievalTimeMs)
                .generationTimeMs(generationTimeMs)
                .totalTimeMs(totalTimeMs)
                .build();
    }
}
