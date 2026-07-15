package com.memoraai.concept.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.chat.service.LLMService;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.document.entity.Document;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.embedding.repository.DocumentEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConceptExtractionServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private DocumentEmbeddingRepository documentEmbeddingRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private LLMService llmService;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    private ConceptExtractionService conceptExtractionService;

    @Captor
    private ArgumentCaptor<List<Concept>> conceptListCaptor;

    @BeforeEach
    void setUp() {
        conceptExtractionService = new ConceptExtractionService(
                documentRepository,
                documentChunkRepository,
                documentEmbeddingRepository,
                conceptRepository,
                llmService,
                objectMapper
        );
    }

    @Test
    void extractConcepts_ShouldDeduplicateAndSave() {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder().id(docId).originalFileName("AI Architecture.pdf").build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        DocumentChunk chunk1 = DocumentChunk.builder().id(UUID.randomUUID()).chunkIndex(0).chunkText("This is about PyMuPDF.").build();
        DocumentChunk chunk2 = DocumentChunk.builder().id(UUID.randomUUID()).chunkIndex(1).chunkText("Another mention of pymupdf.").build();
        
        when(documentChunkRepository.findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(docId))
                .thenReturn(List.of(chunk1, chunk2));

        String json = "[\n" +
                "  {\"name\":\"PyMuPDF\", \"description\":\"A PDF library.\", \"importance\":0.9, \"difficulty\":0.5},\n" +
                "  {\"name\":\"pymupdf\", \"description\":\"A Python library for PDF extraction.\", \"importance\":0.8, \"difficulty\":0.6}\n" +
                "]";

        when(llmService.generateAnswer(anyString()))
                .thenReturn(Mono.just(json));

        when(documentEmbeddingRepository.findByChunkId(any())).thenReturn(Optional.empty());

        conceptExtractionService.extractConcepts(docId);

        verify(conceptRepository).saveAll(conceptListCaptor.capture());
        
        List<Concept> savedConcepts = conceptListCaptor.getValue();
        assertEquals(1, savedConcepts.size(), "Duplicate concepts should be merged");
        
        Concept concept = savedConcepts.get(0);
        assertEquals("PyMuPDF", concept.getName(), "Name should come from first occurrence");
        assertEquals("A Python library for PDF extraction.", concept.getDescription(), "Longest description should be kept");
        
        // Importance should factor in frequency (2/2 chunks), heading boost (none), avg LLM (0.85)
        assertTrue(concept.getImportanceScore() > 0.0);
        assertTrue(concept.getDifficultyScore() > 0.0);
    }
    
    @Test
    void extractConcepts_ShouldIgnoreGenericTerms() {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder().id(docId).build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        DocumentChunk chunk = DocumentChunk.builder().id(UUID.randomUUID()).chunkIndex(0).chunkText("text").build();
        when(documentChunkRepository.findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(docId))
                .thenReturn(List.of(chunk));

        // Returns one valid concept and one too-short concept ("it")
        String json = "[\n" +
                "  {\"name\":\"Machine Learning\", \"description\":\"AI subset.\", \"importance\":0.9, \"difficulty\":0.5},\n" +
                "  {\"name\":\"it\", \"description\":\"pronoun.\", \"importance\":0.1, \"difficulty\":0.1}\n" +
                "]";

        when(llmService.generateAnswer(anyString())).thenReturn(Mono.just(json));

        conceptExtractionService.extractConcepts(docId);

        verify(conceptRepository).saveAll(conceptListCaptor.capture());
        List<Concept> savedConcepts = conceptListCaptor.getValue();
        
        assertEquals(1, savedConcepts.size());
        assertEquals("Machine Learning", savedConcepts.get(0).getName());
    }
}
