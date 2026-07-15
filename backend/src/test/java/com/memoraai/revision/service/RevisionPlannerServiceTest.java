package com.memoraai.revision.service;

import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.revision.dto.RevisionConceptDto;
import com.memoraai.revision.dto.RevisionPlanDto;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionPlannerServiceTest {

    @Mock
    private ANMEMemoryService memoryService;

    @InjectMocks
    private RevisionPlannerService revisionPlannerService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getTodaysRevisionPlan_returnsConceptsOrderedByPriority() {
        Concept conceptA = buildConcept("Recursion", 0.9, 0.8);
        Concept conceptB = buildConcept("Variables", 0.5, 0.3);

        UserMemoryState stateA = buildState(conceptA, 0.2); // low memory → high priority
        UserMemoryState stateB = buildState(conceptB, 0.7); // higher memory → lower priority

        when(memoryService.getRevisionCandidates(userId)).thenReturn(List.of(stateA, stateB));

        RevisionPlanDto plan = revisionPlannerService.getTodaysRevisionPlan(userId);

        assertNotNull(plan);
        assertNotNull(plan.getRevisionDate());
        assertEquals(2, plan.getConcepts().size());

        // First concept should have higher priority
        RevisionConceptDto first = plan.getConcepts().get(0);
        RevisionConceptDto second = plan.getConcepts().get(1);
        assertTrue(first.getPriority() >= second.getPriority(),
                "Concepts should be sorted by priority descending");
    }

    @Test
    void getTodaysRevisionPlan_priorityFormula_isCorrect() {
        Concept concept = buildConcept("Machine Learning", 0.8, 0.6);
        UserMemoryState state = buildState(concept, 0.4);

        when(memoryService.getRevisionCandidates(userId)).thenReturn(List.of(state));

        RevisionPlanDto plan = revisionPlannerService.getTodaysRevisionPlan(userId);
        RevisionConceptDto dto = plan.getConcepts().get(0);

        // priority = 0.50*(1-0.4) + 0.30*0.8 + 0.20*0.6 = 0.30 + 0.24 + 0.12 = 0.66
        double expected = 0.50 * (1 - 0.4) + 0.30 * 0.8 + 0.20 * 0.6;
        assertEquals(expected, dto.getPriority(), 0.001);
    }

    @Test
    void getTodaysRevisionPlan_emptyWhenNoCandidates() {
        when(memoryService.getRevisionCandidates(userId)).thenReturn(List.of());

        RevisionPlanDto plan = revisionPlannerService.getTodaysRevisionPlan(userId);

        assertNotNull(plan);
        assertTrue(plan.getConcepts().isEmpty());
    }

    @Test
    void getTodaysRevisionPlan_priorityClampsToMax1() {
        Concept concept = buildConcept("Advanced Topic", 1.0, 1.0);
        UserMemoryState state = buildState(concept, 0.0);

        when(memoryService.getRevisionCandidates(userId)).thenReturn(List.of(state));

        RevisionPlanDto plan = revisionPlannerService.getTodaysRevisionPlan(userId);
        RevisionConceptDto dto = plan.getConcepts().get(0);

        // priority = 0.50*1.0 + 0.30*1.0 + 0.20*1.0 = 1.0
        assertEquals(1.0, dto.getPriority(), 0.001);
        assertTrue(dto.getPriority() <= 1.0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Concept buildConcept(String name, double importance, double difficulty) {
        return Concept.builder()
                .id(UUID.randomUUID())
                .name(name)
                .importanceScore(importance)
                .difficultyScore(difficulty)
                .build();
    }

    private UserMemoryState buildState(Concept concept, double memoryScore) {
        return UserMemoryState.builder()
                .id(UUID.randomUUID())
                .concept(concept)
                .memoryScore(memoryScore)
                .reviewCount(1)
                .lastReviewedAt(LocalDateTime.now().minusDays(1))
                .nextReviewAt(LocalDateTime.now().minusHours(1))
                .build();
    }
}
