package com.memoraai.anme.service;

import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.anme.repository.UserMemoryStateRepository;
import com.memoraai.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic memory engine for MemoraAI.
 *
 * Memory score is a value between 0.0 (forgotten) and 1.0 (perfectly remembered).
 * Initial score = 0.50
 *
 * On quiz submission:
 *  percentage >= 80 → +0.20
 *  percentage >= 60 → +0.10
 *  percentage <  60 → -0.20
 *
 * Next review schedule:
 *  score >= 0.80 → 7 days
 *  score >= 0.60 → 3 days
 *  otherwise     → 1 day
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ANMEMemoryService {

    private static final double INITIAL_MEMORY_SCORE = 0.50;
    private static final double HIGH_THRESHOLD = 0.80;
    private static final double MID_THRESHOLD  = 0.60;

    private final UserMemoryStateRepository userMemoryStateRepository;
    private final ConceptRepository conceptRepository;

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    /**
     * Creates a UserMemoryState for every concept in the document for the given user.
     * Idempotent — skips concepts that already have a state.
     */
    @Transactional
    public void initializeMemoryStates(UUID documentId, UUID userId, User user) {
        List<Concept> concepts = conceptRepository.findByDocumentId(documentId);
        if (concepts.isEmpty()) {
            log.warn("No concepts found for document {} — skipping memory initialization", documentId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int created = 0;

        for (Concept concept : concepts) {
            if (userMemoryStateRepository.existsByUserIdAndConceptId(userId, concept.getId())) {
                continue;
            }

            UserMemoryState state = UserMemoryState.builder()
                    .user(user)
                    .concept(concept)
                    .memoryScore(INITIAL_MEMORY_SCORE)
                    .reviewCount(0)
                    .lastReviewedAt(now)
                    .nextReviewAt(now)
                    .build();

            userMemoryStateRepository.save(state);
            created++;
        }

        log.info("Initialized {} memory states for document {} user {}", created, documentId, userId);
    }

    // -------------------------------------------------------------------------
    // Memory update after quiz
    // -------------------------------------------------------------------------

    /**
     * Updates a UserMemoryState after a quiz submission using the deterministic formula.
     *
     * @param state      the existing memory state to update
     * @param percentage the quiz percentage score (0–100)
     * @return the saved, updated state
     */
    @Transactional
    public UserMemoryState updateMemoryAfterQuiz(UserMemoryState state, double percentage) {
        double score = state.getMemoryScore();

        if (percentage >= 80.0) {
            score += 0.20;
        } else if (percentage >= 60.0) {
            score += 0.10;
        } else {
            score -= 0.20;
        }

        // Clamp to [0.0, 1.0]
        score = Math.min(1.0, Math.max(0.0, score));

        LocalDateTime now = LocalDateTime.now();

        state.setMemoryScore(score);
        state.setReviewCount(state.getReviewCount() + 1);
        state.setLastReviewedAt(now);
        state.setNextReviewAt(calculateNextReviewAt(score, now));

        return userMemoryStateRepository.save(state);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserMemoryState> getUserMemoryStates(UUID userId) {
        return userMemoryStateRepository.findByUserIdAndConceptDocumentIsDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<UserMemoryState> getRevisionCandidates(UUID userId) {
        return userMemoryStateRepository.findRevisionCandidates(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<UserMemoryState> findByUserAndConcept(UUID userId, UUID conceptId) {
        return userMemoryStateRepository.findByUserIdAndConceptId(userId, conceptId);
    }

    @Transactional(readOnly = true)
    public List<UserMemoryState> getMemoryStatesByDocument(UUID userId, UUID documentId) {
        return userMemoryStateRepository.findByUserIdAndConceptDocumentId(userId, documentId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private LocalDateTime calculateNextReviewAt(double score, LocalDateTime from) {
        if (score >= HIGH_THRESHOLD) {
            return from.plusDays(7);
        } else if (score >= MID_THRESHOLD) {
            return from.plusDays(3);
        } else {
            return from.plusDays(1);
        }
    }
}
