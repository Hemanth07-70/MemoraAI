package com.memoraai.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.search.dto.SearchRequest;
import com.memoraai.search.dto.SearchResultItem;
import com.memoraai.search.service.CosineSimilarityService;
import com.memoraai.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    @MockBean
    private com.memoraai.auth.util.JwtUtil jwtUtil;

    @MockBean
    private com.memoraai.auth.service.AuthenticationService authenticationService;

    @MockBean
    private com.memoraai.profile.service.UserProfileService userProfileService;

    @Test
    void testSearch_Success() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .query("machine learning")
                .topK(2)
                .build();

        SearchResultItem item1 = SearchResultItem.builder()
                .documentId(UUID.randomUUID())
                .chunkId(UUID.randomUUID())
                .score(0.95)
                .text("Machine learning is a subset of AI.")
                .build();

        com.memoraai.search.dto.SearchResponse response = com.memoraai.search.dto.SearchResponse.builder()
                .results(Arrays.asList(item1))
                .build();

        when(searchService.search(org.mockito.ArgumentMatchers.any(SearchRequest.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].score").value(0.95))
                .andExpect(jsonPath("$.results[0].text").value("Machine learning is a subset of AI."));
    }
    
    @Test
    void testSearch_BlankQuery_ReturnsBadRequest() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .query("")
                .topK(5)
                .build();
                
        mockMvc.perform(post("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
