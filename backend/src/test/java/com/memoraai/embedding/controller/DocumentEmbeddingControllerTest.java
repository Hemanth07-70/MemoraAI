package com.memoraai.embedding.controller;

import com.memoraai.document.entity.Document;
import com.memoraai.document.service.DocumentService;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.user.entity.User;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentEmbeddingController.class)
@Import(com.memoraai.auth.util.JwtUtil.class)
class DocumentEmbeddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private DocumentService documentService;

    private User mockUser;
    private Document mockDocument;
    private DocumentChunk mockChunk;
    private DocumentEmbedding mockEmbedding;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(UUID.randomUUID()).email("test@example.com").build();
        mockDocument = Document.builder().id(UUID.randomUUID()).owner(mockUser).build();
        
        ExtractedDocument mockExtractedDoc = ExtractedDocument.builder().document(mockDocument).build();
        mockChunk = DocumentChunk.builder().id(UUID.randomUUID()).extractedDocument(mockExtractedDoc).build();
        
        mockEmbedding = DocumentEmbedding.builder()
                .id(UUID.randomUUID())
                .chunk(mockChunk)
                .dimension(384)
                .modelName("test-model")
                .embeddingJson("[0.1, 0.2, 0.3]")
                .generationTimeMs(100L)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getDocumentEmbeddings_Success() throws Exception {
        when(documentService.getDocumentById(eq(mockDocument.getId()), any())).thenReturn(mockDocument);
        when(embeddingService.getEmbeddingsForDocument(mockDocument.getId())).thenReturn(List.of(mockEmbedding));

        mockMvc.perform(get("/api/v1/documents/" + mockDocument.getId() + "/embeddings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mockEmbedding.getId().toString()))
                .andExpect(jsonPath("$[0].dimension").value(384))
                .andExpect(jsonPath("$[0].modelName").value("test-model"))
                .andExpect(jsonPath("$[0].embeddingJson").doesNotExist());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getChunkEmbedding_Success() throws Exception {
        when(embeddingService.getEmbeddingForChunk(mockChunk.getId())).thenReturn(Optional.of(mockEmbedding));
        when(documentService.getDocumentById(eq(mockDocument.getId()), any())).thenReturn(mockDocument);

        mockMvc.perform(get("/api/v1/chunks/" + mockChunk.getId() + "/embedding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mockEmbedding.getId().toString()))
                .andExpect(jsonPath("$.dimension").value(384))
                .andExpect(jsonPath("$.modelName").value("test-model"))
                .andExpect(jsonPath("$.embeddingJson").value("[0.1, 0.2, 0.3]"));
    }
}
