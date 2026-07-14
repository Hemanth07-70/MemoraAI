package com.memoraai.embedding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.embedding.config.EmbeddingProperties;
import com.memoraai.embedding.dto.EmbeddingRequest;
import com.memoraai.embedding.dto.EmbeddingResponse;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.repository.DocumentEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private DocumentEmbeddingRepository embeddingRepository;
    @Mock
    private EmbeddingProperties embeddingProperties;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper = new ObjectMapper();
    private EmbeddingService embeddingService;

    @Captor
    private ArgumentCaptor<DocumentEmbedding> embeddingCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        
        embeddingService = new EmbeddingService(
                embeddingRepository,
                embeddingProperties,
                webClientBuilder,
                objectMapper,
                "http://localhost:8000"
        );
    }

    @Test
    void generateAndPersistEmbedding_Success() throws Exception {
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .chunkText("Hello world")
                .build();

        when(embeddingRepository.existsByChunkId(chunkId)).thenReturn(false);
        when(embeddingProperties.getModel()).thenReturn("test-model");

        EmbeddingResponse response = EmbeddingResponse.builder()
                .success(true)
                .dimension(384)
                .embedding(List.of(0.1f, 0.2f, 0.3f))
                .build();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/v1/embeddings")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(EmbeddingRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(EmbeddingResponse.class)).thenReturn(Mono.just(response));

        embeddingService.generateAndPersistEmbedding(chunk);

        verify(embeddingRepository).save(embeddingCaptor.capture());
        DocumentEmbedding saved = embeddingCaptor.getValue();
        
        assertEquals(chunk, saved.getChunk());
        assertEquals(384, saved.getDimension());
        assertEquals("test-model", saved.getModelName());
        assertTrue(saved.getEmbeddingJson().contains("0.1"));
    }

    @Test
    void generateAndPersistEmbedding_SkipIfAlreadyExists() {
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .chunkText("Hello world")
                .build();

        when(embeddingRepository.existsByChunkId(chunkId)).thenReturn(true);

        embeddingService.generateAndPersistEmbedding(chunk);

        verify(webClient, never()).post();
        verify(embeddingRepository, never()).save(any());
    }

    @Test
    void generateAndPersistEmbedding_SkipIfEmptyText() {
        DocumentChunk chunk = DocumentChunk.builder()
                .id(UUID.randomUUID())
                .chunkText("   ")
                .build();

        embeddingService.generateAndPersistEmbedding(chunk);

        verify(embeddingRepository, never()).existsByChunkId(any());
        verify(webClient, never()).post();
        verify(embeddingRepository, never()).save(any());
    }
}
