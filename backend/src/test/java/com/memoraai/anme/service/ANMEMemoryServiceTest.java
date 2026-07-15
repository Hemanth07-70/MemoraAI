package com.memoraai.anme.service;

import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.anme.repository.UserMemoryStateRepository;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ANMEMemoryServiceTest {

    @Mock
    private UserMemoryStateRepository userMemoryStateRepository;

    @Mock
    private ConceptRepository conceptRepository;

    @InjectMocks
    private ANMEMemoryService memoryService;

    private User user;
    private Concept concept;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        concept = Concept.builder()
                .id(UUID.randomUUID())
                .difficultyScore(0.5)
                .build();
    }

    @Test
    void initializeMemoryStates_createsMissingStates() {
        UUID documentId = UUID.randomUUID();
        when(conceptRepository.findByDocumentId(documentId)).thenReturn(List.of(concept));
        when(userMemoryStateRepository.existsByUserIdAndConceptId(user.getId(), concept.getId())).thenReturn(false);
        when(userMemoryStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        memoryService.initializeMemoryStates(documentId, user.getId(), user);

        verify(userMemoryStateRepository, times(1)).save(any(UserMemoryState.class));
    }

    @Test
    void initializeMemoryStates_skipsDuplicates() {
        UUID documentId = UUID.randomUUID();
        when(conceptRepository.findByDocumentId(documentId)).thenReturn(List.of(concept));
        when(userMemoryStateRepository.existsByUserIdAndConceptId(user.getId(), concept.getId())).thenReturn(true);

        memoryService.initializeMemoryStates(documentId, user.getId(), user);

        verify(userMemoryStateRepository, never()).save(any());
    }

    @Test
    void initializeMemoryStates_doesNothingWhenNoConcepts() {
        UUID documentId = UUID.randomUUID();
        when(conceptRepository.findByDocumentId(documentId)).thenReturn(Collections.emptyList());

        memoryService.initializeMemoryStates(documentId, user.getId(), user);

        verify(userMemoryStateRepository, never()).save(any());
    }

    @Test
    void updateMemoryAfterQuiz_highPerformance_increasesScore() {
        UserMemoryState state = buildState(0.50);
        when(userMemoryStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 85.0);

        assertEquals(0.70, updated.getMemoryScore(), 0.001);
        assertEquals(1, updated.getReviewCount());
        assertNotNull(updated.getNextReviewAt());
    }

    @Test
    void updateMemoryAfterQuiz_lowPerformance_decreasesScore() {
        UserMemoryState state = buildState(0.50);
        when(userMemoryStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 30.0);

        assertEquals(0.30, updated.getMemoryScore(), 0.001);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserMemoryState buildState(double score) {
        return UserMemoryState.builder()
                .id(UUID.randomUUID())
                .user(user)
                .concept(concept)
                .memoryScore(score)
                .reviewCount(0)
                .lastReviewedAt(LocalDateTime.now().minusDays(1))
                .nextReviewAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
