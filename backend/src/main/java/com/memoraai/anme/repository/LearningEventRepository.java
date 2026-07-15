package com.memoraai.anme.repository;

import com.memoraai.anme.entity.LearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningEventRepository extends JpaRepository<LearningEvent, UUID> {
    List<LearningEvent> findByUserId(UUID userId);
    List<LearningEvent> findByUserIdAndConceptId(UUID userId, UUID conceptId);
}
