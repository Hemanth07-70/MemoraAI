package com.memoraai.search.service;

import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.search.dto.SearchRequest;
import com.memoraai.search.dto.SearchResponse;
import com.memoraai.search.dto.SearchResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final EmbeddingService embeddingService;
    private final CosineSimilarityService cosineSimilarityService;

    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request, com.memoraai.user.entity.User user) {
        long startTime = System.currentTimeMillis();
        log.info("Generating query embedding...");
        
        List<Float> queryEmbedding = embeddingService.generateQueryEmbedding(request.getQuery());
        
        long embeddingTime = System.currentTimeMillis();
        
        List<SearchResultItem> results = cosineSimilarityService.findSimilarChunks(queryEmbedding, request.getTopK() != null ? request.getTopK() : 5, user.getId());
        
        long endTime = System.currentTimeMillis();
        log.info("Cosine similarity completed in {} ms", (endTime - embeddingTime));
        log.info("Returning Top {} results. Total search latency: {} ms", results.size(), (endTime - startTime));
        
        return SearchResponse.builder()
                .results(results)
                .build();
    }
}
