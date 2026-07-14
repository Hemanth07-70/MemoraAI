package com.memoraai.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.document.entity.Document;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.repository.DocumentEmbeddingRepository;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import com.memoraai.search.dto.SearchResultItem;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CosineSimilarityServiceTest {

    @Mock
    private DocumentEmbeddingRepository embeddingRepository;

    private ObjectMapper objectMapper;
    private CosineSimilarityService cosineSimilarityService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cosineSimilarityService = new CosineSimilarityService(embeddingRepository, objectMapper);
    }

    @Test
    void testComputeCosineSimilarity() {
        float[] v1 = {1.0f, 0.0f, 0.0f};
        float[] v2 = {0.0f, 1.0f, 0.0f};
        float[] v3 = {1.0f, 1.0f, 0.0f};
        float[] v4 = {2.0f, 0.0f, 0.0f};

        // Orthogonal
        assertEquals(0.0, cosineSimilarityService.computeCosineSimilarity(v1, v2), 0.0001);
        
        // Same direction (collinear)
        assertEquals(1.0, cosineSimilarityService.computeCosineSimilarity(v1, v1), 0.0001);
        assertEquals(1.0, cosineSimilarityService.computeCosineSimilarity(v1, v4), 0.0001);

        // 45 degrees
        assertEquals(0.7071, cosineSimilarityService.computeCosineSimilarity(v1, v3), 0.0001);
    }

    @Test
    void testFindSimilarChunks_EmptyDatabase() {
        when(embeddingRepository.findAll()).thenReturn(Collections.emptyList());

        List<Float> query = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<SearchResultItem> results = cosineSimilarityService.findSimilarChunks(query, 5, UUID.randomUUID());

        assertTrue(results.isEmpty());
    }

    @Test
    void testFindSimilarChunks_EmptyQuery() {
        List<SearchResultItem> results = cosineSimilarityService.findSimilarChunks(Collections.emptyList(), 5, UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindSimilarChunks_RankingAndTopK() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Document document = Document.builder().id(docId).owner(user).build();
        ExtractedDocument extractedDocument = ExtractedDocument.builder().document(document).build();

        DocumentEmbedding emb1 = createMockEmbedding(extractedDocument, "[1.0, 0.0, 0.0]", "Match 1");
        DocumentEmbedding emb2 = createMockEmbedding(extractedDocument, "[0.0, 1.0, 0.0]", "Match 2");
        DocumentEmbedding emb3 = createMockEmbedding(extractedDocument, "[0.9, 0.1, 0.0]", "Match 3");
        DocumentEmbedding emb4 = createMockEmbedding(extractedDocument, "[0.1, 0.9, 0.0]", "Match 4");

        when(embeddingRepository.findAll()).thenReturn(Arrays.asList(emb1, emb2, emb3, emb4));

        List<Float> query = Arrays.asList(1.0f, 0.0f, 0.0f);
        
        // Find top 2
        List<SearchResultItem> results = cosineSimilarityService.findSimilarChunks(query, 2, userId);

        assertEquals(2, results.size());
        
        // Expected order: emb1 (score 1.0), emb3 (score ~0.99)
        assertEquals(emb1.getChunk().getId(), results.get(0).getChunkId());
        assertEquals("Match 1", results.get(0).getText());
        assertEquals(1.0, results.get(0).getScore(), 0.0001);

        assertEquals(emb3.getChunk().getId(), results.get(1).getChunkId());
        assertEquals("Match 3", results.get(1).getText());
        assertTrue(results.get(1).getScore() > 0.9 && results.get(1).getScore() < 1.0);
    }

    private DocumentEmbedding createMockEmbedding(ExtractedDocument doc, String json, String text) {
        DocumentChunk chunk = DocumentChunk.builder()
                .id(UUID.randomUUID())
                .extractedDocument(doc)
                .chunkText(text)
                .build();

        return DocumentEmbedding.builder()
                .id(UUID.randomUUID())
                .chunk(chunk)
                .embeddingJson(json)
                .build();
    }
}
