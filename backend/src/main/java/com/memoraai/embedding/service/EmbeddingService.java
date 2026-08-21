package com.memoraai.embedding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.embedding.config.EmbeddingProperties;
import com.memoraai.embedding.dto.EmbeddingRequest;
import com.memoraai.embedding.dto.EmbeddingResponse;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.repository.DocumentEmbeddingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class EmbeddingService {

    private final DocumentEmbeddingRepository embeddingRepository;
    private final EmbeddingProperties embeddingProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(
            DocumentEmbeddingRepository embeddingRepository,
            EmbeddingProperties embeddingProperties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${memoraai.ai.base-url}") String aiBaseUrl) {
        this.embeddingRepository = embeddingRepository;
        this.embeddingProperties = embeddingProperties;
        this.webClient = webClientBuilder.baseUrl(aiBaseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void generateAndPersistEmbedding(DocumentChunk chunk) {
        if (chunk == null || chunk.getChunkText() == null || chunk.getChunkText().trim().isEmpty()) {
            log.warn("Skipping embedding generation for empty or null chunk {}", chunk != null ? chunk.getId() : "null");
            return;
        }

        if (embeddingRepository.existsByChunkId(chunk.getId())) {
            log.info("Embedding already exists for chunk {}. Skipping.", chunk.getId());
            return;
        }

        log.info("Embedding generation started for chunk {}", chunk.getId());
        long startTime = System.currentTimeMillis();

        EmbeddingRequest request = EmbeddingRequest.builder()
                .chunkId(chunk.getId().toString())
                .text(chunk.getChunkText())
                .build();

        try {
            EmbeddingResponse response = webClient.post()
                    .uri("/api/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                String embeddingJson = objectMapper.writeValueAsString(response.getEmbedding());
                long generationTime = System.currentTimeMillis() - startTime;

                DocumentEmbedding embedding = DocumentEmbedding.builder()
                        .chunk(chunk)
                        .dimension(response.getDimension())
                        .embeddingJson(embeddingJson)
                        .modelName(embeddingProperties.getModel())
                        .generatedAt(Instant.now())
                        .generationTimeMs(generationTime)
                        .build();

                embeddingRepository.save(embedding);
                // Write vector column separately — requires explicit CAST in native SQL
                embeddingRepository.updateEmbeddingVector(embedding.getId(), embeddingJson);
                log.info("Embedding stored for chunk {} (Dimension: {}, Time: {}ms)", 
                        chunk.getId(), response.getDimension(), generationTime);
            } else {
                String error = response != null ? response.getError() : "Unknown error";
                log.error("Failed to generate embedding for chunk {}: {}", chunk.getId(), error);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize embedding vector for chunk {}: {}", chunk.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error communicating with AI service for chunk {}: {}", chunk.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentEmbedding> getEmbeddingsForDocument(UUID documentId) {
        return embeddingRepository.findByChunkExtractedDocumentDocumentId(documentId);
    }

    @Transactional(readOnly = true)
    public Optional<DocumentEmbedding> getEmbeddingForChunk(UUID chunkId) {
        return embeddingRepository.findByChunkId(chunkId);
    }

    public List<Float> generateQueryEmbedding(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        EmbeddingRequest request = EmbeddingRequest.builder()
                .chunkId("query")
                .text(query)
                .build();

        EmbeddingResponse response = webClient.post()
                .uri("/api/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        if (response != null && response.isSuccess()) {
            return response.getEmbedding();
        } else {
            String error = response != null ? response.getError() : "Unknown error";
            log.error("Failed to generate embedding for query: {}", error);
            throw new RuntimeException("Failed to generate embedding for query: " + error);
        }
    }
}
