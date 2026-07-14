package com.memoraai.chunking.controller;

import com.memoraai.chunking.dto.ChunkResponse;
import com.memoraai.chunking.dto.ChunkStatisticsResponse;
import com.memoraai.chunking.service.DocumentChunkingService;
import com.memoraai.document.entity.Document;
import com.memoraai.document.service.DocumentService;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.memoraai.auth.util.JwtUtil;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentChunkController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test
class DocumentChunkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private DocumentChunkingService chunkingService;

    @MockBean
    private DocumentService documentService;

    private UUID documentId;
    private Document mockDocument;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        mockDocument = Document.builder().id(documentId).owner(new User()).build();
    }

    @Test
    @WithMockUser
    void testGetChunks() throws Exception {
        when(documentService.getDocumentById(eq(documentId), any())).thenReturn(mockDocument);
        
        ChunkResponse chunk = ChunkResponse.builder()
                .id(UUID.randomUUID())
                .chunkIndex(0)
                .chunkText("Test chunk")
                .build();
                
        when(chunkingService.getChunksForDocument(documentId)).thenReturn(List.of(chunk));

        mockMvc.perform(get("/api/v1/documents/{id}/chunks", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chunkIndex").value(0))
                .andExpect(jsonPath("$[0].chunkText").value("Test chunk"));
    }

    @Test
    @WithMockUser
    void testGetChunkStatistics() throws Exception {
        when(documentService.getDocumentById(eq(documentId), any())).thenReturn(mockDocument);
        
        ChunkStatisticsResponse stats = ChunkStatisticsResponse.builder()
                .chunkCount(5)
                .averageChunkSize(100)
                .build();
                
        when(chunkingService.getChunkStatistics(documentId)).thenReturn(stats);

        mockMvc.perform(get("/api/v1/documents/{id}/chunks/statistics", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunkCount").value(5))
                .andExpect(jsonPath("$.averageChunkSize").value(100));
    }
}
