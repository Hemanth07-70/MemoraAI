package com.memoraai.anme.repository;

import com.memoraai.anme.entity.UserMemoryState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMemoryStateRepository extends JpaRepository<UserMemoryState, UUID> {

    @EntityGraph(attributePaths = {"concept"})
    Optional<UserMemoryState> findByUserIdAndConceptId(UUID userId, UUID conceptId);

    @EntityGraph(attributePaths = {"concept"})
    List<UserMemoryState> findByUserIdAndConceptDocumentIsDeletedFalse(UUID userId);

    boolean existsByUserIdAndConceptId(UUID userId, UUID conceptId);

    /**
     * Finds concepts due for revision: memoryScore < 0.80 OR nextReviewAt <= now.
     */
    @EntityGraph(attributePaths = {"concept"})
    @Query("SELECT ums FROM UserMemoryState ums WHERE ums.user.id = :userId " +
           "AND (ums.memoryScore < 0.80 OR ums.nextReviewAt <= :now) " +
           "AND ums.concept.document.isDeleted = false")
    List<UserMemoryState> findRevisionCandidates(@Param("userId") UUID userId,
                                                  @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"concept"})
    List<UserMemoryState> findByUserIdAndConceptDocumentId(UUID userId, UUID documentId);
}
