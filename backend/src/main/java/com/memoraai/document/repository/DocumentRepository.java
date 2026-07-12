package com.memoraai.document.repository;

import com.memoraai.document.entity.Document;
import com.memoraai.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByOwnerAndIsDeletedFalse(User owner);
    Optional<Document> findByIdAndOwnerAndIsDeletedFalse(UUID id, User owner);
}
