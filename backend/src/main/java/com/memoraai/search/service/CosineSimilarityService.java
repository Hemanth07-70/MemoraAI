package com.memoraai.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.repository.DocumentEmbeddingRepository;
import com.memoraai.search.dto.SearchResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CosineSimilarityService {

    private final DocumentEmbeddingRepository embeddingRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SearchResultItem> findSimilarChunks(List<Float> queryEmbeddingList, int topK, UUID userId) {
        return findSimilarChunks(queryEmbeddingList, topK, null, userId);
    }

    @Transactional(readOnly = true)
    public List<SearchResultItem> findSimilarChunks(
            List<Float> queryEmbeddingList, int topK, UUID documentId, UUID userId) {

        if (queryEmbeddingList == null || queryEmbeddingList.isEmpty()) {
            return Collections.emptyList();
        }

        // Serialise query vector to pgvector text format: [-0.1,0.2,...]
        String queryVector;
        try {
            queryVector = objectMapper.writeValueAsString(queryEmbeddingList);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise query embedding: {}", e.getMessage());
            return Collections.emptyList();
        }

        List<Object[]> rows;
        if (documentId != null) {
            rows = embeddingRepository.vectorSearchForDocument(queryVector, documentId, userId, topK);
        } else {
            rows = embeddingRepository.vectorSearchForUser(queryVector, userId, topK);
        }

        log.info("pgvector HNSW search returned {} results", rows.size());

        return rows.stream().map(row -> SearchResultItem.builder()
                .documentId(toUUID(row[0]))
                .chunkId(toUUID(row[1]))
                .chunkIndex(((Number) row[2]).intValue())
                .text((String) row[3])
                .score(((Number) row[4]).doubleValue())
                .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Kept for unit tests / internal use
    // -------------------------------------------------------------------------
    protected double computeCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < vectorA.length; i++) {
            dot   += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private UUID toUUID(Object o) {
        if (o instanceof UUID) return (UUID) o;
        return UUID.fromString(o.toString());
    }
}
