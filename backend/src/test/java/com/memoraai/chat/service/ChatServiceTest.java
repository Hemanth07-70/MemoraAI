package com.memoraai.chat.service;

import com.memoraai.anme.repository.ConceptRelationshipRepository;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.chat.dto.ChatAskRequest;
import com.memoraai.chat.dto.ChatAskResponse;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.search.dto.SearchResultItem;
import com.memoraai.search.service.CosineSimilarityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private CosineSimilarityService cosineSimilarityService;

    @Mock
    private PromptBuilderService promptBuilderService;

    @Mock
    private LLMService llmService;

    @Mock
    private com.memoraai.conversation.service.ConversationService conversationService;

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private ConceptRelationshipRepository conceptRelationshipRepository;

    @Mock
    private ANMEMemoryService anmeMemoryService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        when(conceptRepository.findByDocumentId(any())).thenReturn(Collections.emptyList());
        when(anmeMemoryService.getMemoryStatesByDocument(any(), any())).thenReturn(Collections.emptyList());

        chatService = new ChatService(
                embeddingService,
                cosineSimilarityService,
                promptBuilderService,
                llmService,
                conversationService,
                conceptRepository,
                conceptRelationshipRepository,
                anmeMemoryService,
                0.30
        );
    }

    @Test
    void shouldGenerateAnswerSuccessfully() {
        ChatAskRequest request = ChatAskRequest.builder()
                .question("Test question")
                .topK(5)
                .build();

        com.memoraai.user.entity.User user = com.memoraai.user.entity.User.builder().id(UUID.randomUUID()).build();
        List<Float> embedding = List.of(0.1f, 0.2f);
        when(embeddingService.generateQueryEmbedding("Test question")).thenReturn(embedding);
        
        List<SearchResultItem> results = List.of(SearchResultItem.builder()
                .chunkId(UUID.randomUUID())
                .score(0.85)
                .text("Test context")
                .build());

        when(cosineSimilarityService.findSimilarChunks(anyList(), anyInt(), any(), any())).thenReturn(results);
        when(promptBuilderService.buildPrompt(anyString(), anyList(), any())).thenReturn("test prompt");
        when(llmService.getProviderName()).thenReturn("test-provider");
        when(llmService.getModelName()).thenReturn("test-model");
        when(llmService.generateAnswer(anyString())).thenReturn(Mono.just("test answer"));

        // Act
        ChatAskResponse response = chatService.askQuestion(request, user);

        assertThat(response.getAnswer()).isEqualTo("test answer");
        assertThat(response.getProvider()).isEqualTo("test-provider");
        assertThat(response.getModel()).isEqualTo("test-model");
        assertThat(response.getSources()).hasSize(1);
    }

    @Test
    void shouldPreventHallucinationWhenScoreIsLow() {
        ChatAskRequest request = ChatAskRequest.builder()
                .question("Test question")
                .topK(5)
                .build();

        com.memoraai.user.entity.User user = com.memoraai.user.entity.User.builder().id(UUID.randomUUID()).build();
        List<Float> embedding = List.of(0.1f, 0.2f);
        when(embeddingService.generateQueryEmbedding("Test question")).thenReturn(embedding);

        SearchResultItem item = SearchResultItem.builder()
                .chunkId(UUID.randomUUID())
                .score(0.20) // Below threshold of 0.30
                .text("Irrelevant context")
                .build();
        when(cosineSimilarityService.findSimilarChunks(anyList(), anyInt(), any(), any())).thenReturn(List.of(item));

        ChatAskResponse response = chatService.askQuestion(request, user);

        assertThat(response.getAnswer()).isEqualTo("I couldn't find this information in the uploaded documents.");
        assertThat(response.getSources()).isEmpty();
        assertThat(response.getGenerationTimeMs()).isEqualTo(0);

        verify(llmService, never()).generateAnswer(anyString());
    }
}
