package com.memoraai.revision.service;

import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.revision.dto.RevisionConceptDto;
import com.memoraai.revision.dto.RevisionPlanDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates today's revision queue for a user.
 *
 * Priority formula:
 *   priority = 0.50 × (1 - memoryScore)
 *            + 0.30 × importanceScore
 *            + 0.20 × difficultyScore
 *
 * Only concepts where memoryScore < 0.80 OR nextReviewAt <= now are included.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevisionPlannerService {

    private final ANMEMemoryService memoryService;

    @Transactional(readOnly = true)
    public RevisionPlanDto getTodaysRevisionPlan(UUID userId) {
        List<UserMemoryState> candidates = memoryService.getRevisionCandidates(userId);

        List<RevisionConceptDto> sorted = candidates.stream()
                .map(this::toRevisionConcept)
                .sorted(Comparator.comparingDouble(RevisionConceptDto::getPriority).reversed())
                .collect(Collectors.toList());

        log.info("Revision plan for user {} contains {} concepts", userId, sorted.size());

        return RevisionPlanDto.builder()
                .revisionDate(LocalDate.now())
                .concepts(sorted)
                .build();
    }

    private RevisionConceptDto toRevisionConcept(UserMemoryState state) {
        double memoryScore    = state.getMemoryScore();
        double importanceScore = state.getConcept().getImportanceScore();
        double difficultyScore = state.getConcept().getDifficultyScore();

        double priority = (0.50 * (1.0 - memoryScore))
                        + (0.30 * importanceScore)
                        + (0.20 * difficultyScore);

        // Clamp to [0.0, 1.0]
        priority = Math.min(1.0, Math.max(0.0, priority));

        return RevisionConceptDto.builder()
                .conceptId(state.getConcept().getId())
                .conceptName(state.getConcept().getName())
                .documentId(state.getConcept().getDocument().getId())
                .memoryScore(memoryScore)
                .importanceScore(importanceScore)
                .difficultyScore(difficultyScore)
                .priority(priority)
                .build();
    }
}
