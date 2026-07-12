package com.memoraai.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.document.dto.DocumentResponse;
import com.memoraai.document.entity.Document;
import com.memoraai.document.entity.DocumentStatus;
import com.memoraai.document.mapper.DocumentMapper;
import com.memoraai.document.service.DocumentService;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.memoraai.auth.util.JwtUtil;
import com.memoraai.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentMapper documentMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private Document testDocument;
    private DocumentResponse documentResponse;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(UUID.randomUUID()).email("test@test.com").build();
        documentId = UUID.randomUUID();
        
        testDocument = Document.builder()
                .id(documentId)
                .fileName("uuid.pdf")
                .originalFileName("test.pdf")
                .build();
                
        documentResponse = DocumentResponse.builder()
                .id(documentId)
                .originalFileName("test.pdf")
                .status(DocumentStatus.UPLOADED)
                .build();
    }

    @Test
    void uploadDocument_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        
        when(documentService.uploadDocument(any(), any())).thenReturn(testDocument);
        when(documentMapper.toResponse(testDocument)).thenReturn(documentResponse);
        
        mockMvc.perform(multipart("/api/v1/documents/upload")
                .file(file)
                .with(user("test@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    void listDocuments_success() throws Exception {
        when(documentService.getUserDocuments(any())).thenReturn(List.of(testDocument));
        when(documentMapper.toResponseList(any())).thenReturn(List.of(documentResponse));
        
        mockMvc.perform(get("/api/v1/documents")
                .with(user("test@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documents[0].id").value(documentId.toString()));
    }

    @Test
    void deleteDocument_success() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/" + documentId)
                .with(user("test@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
