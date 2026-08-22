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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /** Batch-embeds all chunks in one HTTP call — replaces the per-chunk loop. */
    @Transactional
    public void generateAndPersistEmbeddingsBatch(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;

        List<DocumentChunk> toProcess = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            if (chunk == null || chunk.getChunkText() == null || chunk.getChunkText().trim().isEmpty()) continue;
            if (embeddingRepository.existsByChunkId(chunk.getId())) continue;
            toProcess.add(chunk);
        }
        if (toProcess.isEmpty()) return;

        log.info("Batch embedding {} chunks in one AI service call", toProcess.size());
        long startTime = System.currentTimeMillis();

        List<Map<String, String>> requestItems = toProcess.stream()
                .map(c -> Map.of("chunkId", c.getId().toString(), "text", c.getChunkText()))
                .toList();

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/embeddings/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestItems)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                log.error("Batch embedding failed: {}", response);
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Batch embedding completed: {} embeddings in {}ms", results.size(), elapsed);

            Map<String, DocumentChunk> chunkById = new java.util.HashMap<>();
            toProcess.forEach(c -> chunkById.put(c.getId().toString(), c));

            for (Map<String, Object> r : results) {
                String chunkId = (String) r.get("chunkId");
                @SuppressWarnings("unchecked")
                List<Double> vec = (List<Double>) r.get("embedding");
                int dim = ((Number) r.get("dimension")).intValue();
                DocumentChunk chunk = chunkById.get(chunkId);
                if (chunk == null || vec == null) continue;
                try {
                    String embeddingJson = objectMapper.writeValueAsString(vec);
                    DocumentEmbedding embedding = DocumentEmbedding.builder()
                            .chunk(chunk)
                            .dimension(dim)
                            .embeddingJson(embeddingJson)
                            .modelName(embeddingProperties.getModel())
                            .generatedAt(Instant.now())
                            .generationTimeMs(elapsed / toProcess.size())
                            .build();
                    embeddingRepository.save(embedding);
                    embeddingRepository.updateEmbeddingVector(embedding.getId(), embeddingJson);
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize embedding for chunk {}: {}", chunkId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Batch embedding request failed: {}", e.getMessage());
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
