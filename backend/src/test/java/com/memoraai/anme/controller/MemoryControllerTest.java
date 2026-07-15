package com.memoraai.anme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.anme.dto.UserMemoryStateDto;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.mapper.ANMEMapper;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import com.memoraai.anme.dto.ConceptDto;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemoryController.class)
class MemoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ANMEMemoryService memoryService;

    @MockBean
    private ANMEMapper anmeMapper;

    @Test
    @WithMockUser
    void getMemoryStates_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UserMemoryState state = UserMemoryState.builder()
                .id(UUID.randomUUID())
                .memoryScore(0.5)
                .reviewCount(1)
                .lastReviewedAt(LocalDateTime.now())
                .nextReviewAt(LocalDateTime.now().plusDays(1))
                .build();

        UserMemoryStateDto dto = UserMemoryStateDto.builder()
                .id(state.getId())
                .userId(userId)
                .memoryScore(0.5)
                .reviewCount(1)
                .build();

        when(memoryService.getUserMemoryStates(any())).thenReturn(List.of(state));
        when(anmeMapper.userMemoryStateToDto(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/memory/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser
    void getMemoryStatesForDocument_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID conceptId = UUID.randomUUID();

        UserMemoryState state = UserMemoryState.builder()
                .id(UUID.randomUUID())
                .memoryScore(0.7)
                .reviewCount(2)
                .build();

        UserMemoryStateDto dto = UserMemoryStateDto.builder()
                .id(state.getId())
                .userId(userId)
                .concept(ConceptDto.builder()
                        .id(conceptId)
                        .name("Test Concept")
                        .importanceScore(0.8)
                        .difficultyScore(0.5)
                        .build())
                .memoryScore(0.7)
                .reviewCount(2)
                .build();

        when(memoryService.getMemoryStatesByDocument(any(), any())).thenReturn(List.of(state));
        when(anmeMapper.userMemoryStateToDto(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/memory/document/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].concept.name").value("Test Concept"))
                .andExpect(jsonPath("$.data[0].memoryScore").value(0.7));
    }

    @Test
    void getMemoryStates_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/memory/me"))
                .andExpect(status().isUnauthorized());
    }
}
