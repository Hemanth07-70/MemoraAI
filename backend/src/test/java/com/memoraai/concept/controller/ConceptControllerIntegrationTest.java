package com.memoraai.concept.controller;

import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.document.entity.Document;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.user.entity.Role;
import com.memoraai.user.entity.User;
import com.memoraai.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConceptControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        conceptRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test.concept@example.com")
                .password("password")
                .role(Role.STUDENT)
                .build();
        userRepository.save(testUser);

        testDocument = Document.builder()
                .owner(testUser)
                .fileName("test-doc.pdf")
                .originalFileName("Test Document.pdf")
                .mimeType("application/pdf")
                .extension("pdf")
                .size(1024L)
                .storagePath("/tmp/test-doc.pdf")
                .status(com.memoraai.document.entity.DocumentStatus.READY)
                .isDeleted(false)
                .uploadedAt(LocalDateTime.now())
                .build();
        documentRepository.save(testDocument);

        Concept concept1 = Concept.builder()
                .document(testDocument)
                .name("Transformer")
                .normalizedName("transformer")
                .description("A neural network architecture.")
                .importanceScore(0.95)
                .difficultyScore(0.84)
                .build();

        Concept concept2 = Concept.builder()
                .document(testDocument)
                .name("Attention")
                .normalizedName("attention")
                .description("Mechanism in transformers.")
                .importanceScore(0.90)
                .difficultyScore(0.70)
                .build();

        conceptRepository.save(concept1);
        conceptRepository.save(concept2);
    }

    @AfterEach
    void tearDown() {
        conceptRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "test.concept@example.com")
    void getDocumentConcepts_ShouldReturnConcepts() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{documentId}/concepts", testDocument.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].importanceScore").exists())
                .andExpect(jsonPath("$.data[0].difficultyScore").exists());
    }
}
