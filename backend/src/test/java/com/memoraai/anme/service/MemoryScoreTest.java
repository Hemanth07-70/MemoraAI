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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryScoreTest {

    @Mock
    private UserMemoryStateRepository userMemoryStateRepository;

    @Mock
    private ConceptRepository conceptRepository;

    @InjectMocks
    private ANMEMemoryService memoryService;

    private User user;
    private Concept concept;
    private UserMemoryState state;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        concept = Concept.builder().id(UUID.randomUUID()).difficultyScore(0.5).build();
        state = UserMemoryState.builder()
                .id(UUID.randomUUID())
                .user(user)
                .concept(concept)
                .memoryScore(0.50)
                .reviewCount(0)
                .lastReviewedAt(LocalDateTime.now().minusDays(1))
                .nextReviewAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userMemoryStateRepository.save(any(UserMemoryState.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // Memory score delta tests
    // -------------------------------------------------------------------------

    @Test
    void updateMemory_highPerformance_increasesScore() {
        // 80+ percent → +0.20
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 85.0);

        assertEquals(0.70, updated.getMemoryScore(), 0.001);
        assertEquals(1, updated.getReviewCount());
        assertNotNull(updated.getLastReviewedAt());
        assertNotNull(updated.getNextReviewAt());
    }

    @Test
    void updateMemory_midPerformance_increasesScoreSmall() {
        // 60-79 → +0.10
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 70.0);

        assertEquals(0.60, updated.getMemoryScore(), 0.001);
    }

    @Test
    void updateMemory_lowPerformance_decreasesScore() {
        // <60 → -0.20
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 40.0);

        assertEquals(0.30, updated.getMemoryScore(), 0.001);
    }

    @Test
    void updateMemory_clampsAt1() {
        state.setMemoryScore(0.90);
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 100.0);

        assertTrue(updated.getMemoryScore() <= 1.0);
        assertEquals(1.0, updated.getMemoryScore(), 0.001);
    }

    @Test
    void updateMemory_clampsAt0() {
        state.setMemoryScore(0.10);
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 0.0);

        assertTrue(updated.getMemoryScore() >= 0.0);
        assertEquals(0.0, updated.getMemoryScore(), 0.001);
    }

    @Test
    void updateMemory_highScore_schedulesIn7Days() {
        state.setMemoryScore(0.70);
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 85.0);

        // score becomes 0.90 (clamped to 1.0 if needed) → 7 days
        assertEquals(0.90, updated.getMemoryScore(), 0.001);
        LocalDateTime expectedNext = updated.getLastReviewedAt().plusDays(7);
        assertEquals(expectedNext.getDayOfYear(), updated.getNextReviewAt().getDayOfYear());
    }

    @Test
    void updateMemory_midScore_schedulesIn3Days() {
        state.setMemoryScore(0.50);
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 70.0);

        // score becomes 0.60 → 3 days
        assertEquals(0.60, updated.getMemoryScore(), 0.001);
        LocalDateTime expectedNext = updated.getLastReviewedAt().plusDays(3);
        assertEquals(expectedNext.getDayOfYear(), updated.getNextReviewAt().getDayOfYear());
    }

    @Test
    void updateMemory_lowScore_schedulesIn1Day() {
        state.setMemoryScore(0.40);
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 30.0);

        // score becomes 0.20 → 1 day
        assertEquals(0.20, updated.getMemoryScore(), 0.001);
        LocalDateTime expectedNext = updated.getLastReviewedAt().plusDays(1);
        assertEquals(expectedNext.getDayOfYear(), updated.getNextReviewAt().getDayOfYear());
    }

    @Test
    void updateMemory_exactBoundary80_incrementsByPoint20() {
        // Exactly 80 → +0.20
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 80.0);
        assertEquals(0.70, updated.getMemoryScore(), 0.001);
    }

    @Test
    void updateMemory_exactBoundary60_incrementsByPoint10() {
        // Exactly 60 → +0.10
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 60.0);
        assertEquals(0.60, updated.getMemoryScore(), 0.001);
    }

    @Test
    void updateMemory_incrementsReviewCount() {
        state.setReviewCount(3);
        UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, 75.0);
        assertEquals(4, updated.getReviewCount());
    }
}
