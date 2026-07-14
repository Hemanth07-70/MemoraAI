package com.memoraai.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.chat.dto.ChatAskRequest;
import com.memoraai.chat.dto.ChatAskResponse;
import com.memoraai.chat.service.ChatService;
import com.memoraai.search.dto.SearchResultItem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private com.memoraai.auth.util.JwtUtil jwtUtil;

    @MockBean
    private com.memoraai.auth.service.AuthenticationService authenticationService;

    @MockBean
    private com.memoraai.profile.service.UserProfileService userProfileService;

    @Test
    void shouldAnswerQuestionSuccessfully() throws Exception {
        UUID docId = UUID.randomUUID();
        SearchResultItem source = SearchResultItem.builder()
                .documentId(docId)
                .chunkId(UUID.randomUUID())
                .chunkIndex(1)
                .score(0.95)
                .text("Java, Python, C, C++")
                .build();

        ChatAskResponse expectedResponse = ChatAskResponse.builder()
                .answer("Java, Python, C, C++")
                .provider("nemotron")
                .model("nvidia/nemotron-3-super")
                .sources(List.of(source))
                .retrievalTimeMs(15)
                .generationTimeMs(800)
                .totalTimeMs(815)
                .build();

        Mockito.when(chatService.askQuestion(any(ChatAskRequest.class), any())).thenReturn(expectedResponse);

        ChatAskRequest request = ChatAskRequest.builder()
                .question("What programming languages does the candidate know?")
                .topK(5)
                .build();

        mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Java, Python, C, C++"))
                .andExpect(jsonPath("$.provider").value("nemotron"))
                .andExpect(jsonPath("$.model").value("nvidia/nemotron-3-super"))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources[0].text").value("Java, Python, C, C++"))
                .andExpect(jsonPath("$.sources[0].documentId").value(docId.toString()));
    }
}
