package com.memoraai.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.repository.DocumentEmbeddingRepository;
import com.memoraai.search.dto.SearchResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CosineSimilarityService {

    private final DocumentEmbeddingRepository embeddingRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<UUID, float[]> vectorCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<SearchResultItem> findSimilarChunks(List<Float> queryEmbeddingList, int topK, UUID userId) {
        return findSimilarChunks(queryEmbeddingList, topK, null, userId);
    }

    @Transactional(readOnly = true)
    public List<SearchResultItem> findSimilarChunks(List<Float> queryEmbeddingList, int topK, UUID documentId, UUID userId) {
        if (queryEmbeddingList == null || queryEmbeddingList.isEmpty()) {
            return Collections.emptyList();
        }

        float[] queryVector = toPrimitiveArray(queryEmbeddingList);
        List<DocumentEmbedding> allEmbeddings = embeddingRepository.findAll();
        
        if (documentId != null) {
            allEmbeddings = allEmbeddings.stream()
                    .filter(emb -> documentId.equals(emb.getChunk().getExtractedDocument().getDocument().getId()))
                    .filter(emb -> emb.getChunk().getExtractedDocument().getDocument().getOwner().getId().equals(userId))
                    .filter(emb -> !emb.getChunk().getExtractedDocument().getDocument().getIsDeleted())
                    .collect(Collectors.toList());
        } else if (userId != null) {
            allEmbeddings = allEmbeddings.stream()
                    .filter(emb -> emb.getChunk().getExtractedDocument().getDocument().getOwner().getId().equals(userId))
                    .filter(emb -> !emb.getChunk().getExtractedDocument().getDocument().getIsDeleted())
                    .collect(Collectors.toList());
        }
        
        log.info("Loaded {} embeddings...", allEmbeddings.size());
        
        PriorityQueue<ScoredEmbedding> minHeap = new PriorityQueue<>(
                Comparator.comparingDouble(ScoredEmbedding::getScore)
        );

        for (DocumentEmbedding embedding : allEmbeddings) {
            try {
                float[] docVector = vectorCache.computeIfAbsent(
                        embedding.getId(),
                        id -> parseVector(embedding.getEmbeddingJson())
                );
                
                double score = computeCosineSimilarity(queryVector, docVector);

                if (minHeap.size() < topK) {
                    minHeap.offer(new ScoredEmbedding(embedding, score));
                } else if (minHeap.peek() != null && score > minHeap.peek().getScore()) {
                    minHeap.poll();
                    minHeap.offer(new ScoredEmbedding(embedding, score));
                }
            } catch (Exception e) {
                log.error("Failed to process embedding for chunk {}: {}", embedding.getChunk().getId(), e.getMessage());
            }
        }

        List<SearchResultItem> results = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            ScoredEmbedding scored = minHeap.poll();
            DocumentEmbedding emb = scored.getEmbedding();
            
            results.add(SearchResultItem.builder()
                    .documentId(emb.getChunk().getExtractedDocument().getDocument().getId())
                    .chunkId(emb.getChunk().getId())
                    .score(scored.getScore())
                    .chunkIndex(emb.getChunk().getChunkIndex())
                    .text(emb.getChunk().getChunkText())
                    .build());
        }

        // The heap gives us results in ascending order, so we reverse it to get descending order
        Collections.reverse(results);
        return results;
    }

    private float[] parseVector(String json) {
        try {
            Float[] wrapperArray = objectMapper.readValue(json, Float[].class);
            float[] primitiveArray = new float[wrapperArray.length];
            for (int i = 0; i < wrapperArray.length; i++) {
                primitiveArray[i] = wrapperArray[i] != null ? wrapperArray[i] : 0.0f;
            }
            return primitiveArray;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse embedding json", e);
        }
    }

    private float[] toPrimitiveArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i) != null ? list.get(i) : 0.0f;
        }
        return array;
    }

    protected double computeCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class ScoredEmbedding {
        private final DocumentEmbedding embedding;
        private final double score;

        public ScoredEmbedding(DocumentEmbedding embedding, double score) {
            this.embedding = embedding;
            this.score = score;
        }

        public DocumentEmbedding getEmbedding() {
            return embedding;
        }

        public double getScore() {
            return score;
        }
    }
}
