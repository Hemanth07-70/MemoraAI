package com.memoraai.conversation.repository;

import com.memoraai.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);
}
