package com.memoraai.chunking.service;

import com.memoraai.chunking.config.ChunkingProperties;
import com.memoraai.chunking.dto.ChunkStatisticsResponse;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.document.entity.Document;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentChunkingServiceTest {

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private ChunkingProperties chunkingProperties;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private DocumentChunkingService documentChunkingService;

    @Captor
    private ArgumentCaptor<List<DocumentChunk>> chunksCaptor;

    private ExtractedDocument mockDocument;

    @BeforeEach
    void setUp() {
        Document doc = Document.builder().id(UUID.randomUUID()).build();
        mockDocument = ExtractedDocument.builder()
                .document(doc)
                .build();
                
        lenient().when(chunkingProperties.getChunkSize()).thenReturn(1000);
        lenient().when(chunkingProperties.getOverlap()).thenReturn(200);
    }

    @Test
    void testChunkDocument_EdgeCaseNoSpaces_6149Chars() {
        // Create a 6149 character string with NO spaces at all.
        // This is the worst-case scenario that triggered the infinite loop.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6149; i++) {
            sb.append('A');
        }
        mockDocument.setExtractedText(sb.toString());

        ChunkStatisticsResponse stats = documentChunkingService.chunkDocument(mockDocument);

        verify(documentChunkRepository).saveAll(chunksCaptor.capture());
        List<DocumentChunk> chunks = chunksCaptor.getValue();

        // With size 1000 and overlap 200, progress is 800 chars per chunk.
        // 6149 / 800 is approx 7-8 chunks.
        assertTrue(chunks.size() >= 7 && chunks.size() <= 9, "Should be approximately 8 chunks");
        
        int prevStartOffset = -1;
        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.getStartOffset() > prevStartOffset, "start_offset must be strictly increasing");
            assertTrue(chunk.getEndOffset() > chunk.getStartOffset(), "chunk must have positive length");
            prevStartOffset = chunk.getStartOffset();
        }
        
        // Ensure no two chunks have the exact same end_offset
        long uniqueEndOffsets = chunks.stream().map(DocumentChunk::getEndOffset).distinct().count();
        assertEquals(chunks.size(), uniqueEndOffsets, "No repeated end_offset values");
    }

    @Test
    void testChunkDocument_NormalText() {
        // A normal string with spaces.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            sb.append("This is a sentence. ");
            for(int j=0; j<20; j++) sb.append("word ");
            sb.append("\n\n");
        }
        mockDocument.setExtractedText(sb.toString());

        ChunkStatisticsResponse stats = documentChunkingService.chunkDocument(mockDocument);

        verify(documentChunkRepository).saveAll(chunksCaptor.capture());
        List<DocumentChunk> chunks = chunksCaptor.getValue();

        assertFalse(chunks.isEmpty());
        for (int i = 1; i < chunks.size(); i++) {
            DocumentChunk prev = chunks.get(i - 1);
            DocumentChunk curr = chunks.get(i);
            
            assertTrue(curr.getStartOffset() > prev.getStartOffset(), "start_offset must strictly increase");
            assertTrue(curr.getStartOffset() < prev.getEndOffset(), "chunks must overlap");
            assertNotEquals(prev.getEndOffset(), curr.getEndOffset(), "end_offsets must differ");
        }
    }
}
