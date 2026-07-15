package com.memoraai.quiz.repository;

import com.memoraai.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findByUserId(UUID userId);
    List<Quiz> findByDocumentIdAndUserId(UUID documentId, UUID userId);
}
