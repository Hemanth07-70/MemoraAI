package com.memoraai.chat.service;

import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.ConceptRelationship;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.repository.ConceptRelationshipRepository;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.chat.dto.ChatAskRequest;
import com.memoraai.chat.dto.ChatAskResponse;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.search.dto.SearchResultItem;
import com.memoraai.search.service.CosineSimilarityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final EmbeddingService embeddingService;
    private final CosineSimilarityService cosineSimilarityService;
    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;
    private final com.memoraai.conversation.service.ConversationService conversationService;
    private final ConceptRepository conceptRepository;
    private final ConceptRelationshipRepository conceptRelationshipRepository;
    private final ANMEMemoryService anmeMemoryService;
    private final double hallucinationThreshold;

    public ChatService(EmbeddingService embeddingService,
                       CosineSimilarityService cosineSimilarityService,
                       PromptBuilderService promptBuilderService,
                       LLMService llmService,
                       com.memoraai.conversation.service.ConversationService conversationService,
                       ConceptRepository conceptRepository,
                       ConceptRelationshipRepository conceptRelationshipRepository,
                       ANMEMemoryService anmeMemoryService,
                       @Value("${memoraai.ai.hallucination-threshold:0.30}") double hallucinationThreshold) {
        this.embeddingService = embeddingService;
        this.cosineSimilarityService = cosineSimilarityService;
        this.promptBuilderService = promptBuilderService;
        this.llmService = llmService;
        this.conversationService = conversationService;
        this.conceptRepository = conceptRepository;
        this.conceptRelationshipRepository = conceptRelationshipRepository;
        this.anmeMemoryService = anmeMemoryService;
        this.hallucinationThreshold = hallucinationThreshold;
    }

    public ChatAskResponse askQuestion(ChatAskRequest request, com.memoraai.user.entity.User user) {
        long startTime = System.currentTimeMillis();
        log.info("Question received: '{}'", request.getQuestion());

        List<com.memoraai.conversation.dto.ChatMessageDto> history = null;
        if (request.getConversationId() != null) {
            history = conversationService.getConversationMessages(request.getConversationId(), user.getId());
            conversationService.addMessageToConversation(
                    request.getConversationId(), user.getId(),
                    com.memoraai.conversation.entity.MessageRole.USER, request.getQuestion());
        }

        // ── Layer 2: KG-guided query expansion ─────────────────────────────
        String expandedQuery = expandQueryWithKG(request.getQuestion(), request.getDocumentId());
        log.info("Expanded query: '{}'", expandedQuery);

        log.info("Generating query embedding (pgvector path)...");
        List<Float> queryEmbedding = embeddingService.generateQueryEmbedding(expandedQuery);

        // ── Layer 1: pgvector HNSW retrieval ───────────────────────────────
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        List<SearchResultItem> retrievedChunks = cosineSimilarityService.findSimilarChunks(
                queryEmbedding, topK, request.getDocumentId(), user.getId());

        long retrievalTimeMs = System.currentTimeMillis() - startTime;
        log.info("pgvector retrieved {} chunks", retrievedChunks.size());

        // ── Layer 3: ANME memory-weighted re-ranking ───────────────────────
        if (!retrievedChunks.isEmpty() && request.getDocumentId() != null) {
            retrievedChunks = reRankWithANME(retrievedChunks, user.getId(), request.getDocumentId());
            log.info("ANME re-ranking applied");
        }

        if (retrievedChunks.isEmpty() || retrievedChunks.get(0).getScore() < hallucinationThreshold) {
            log.info("Best score below threshold ({}), refusing answer", hallucinationThreshold);
            return ChatAskResponse.builder()
                    .answer("I couldn't find this information in the uploaded documents.")
                    .sources(Collections.emptyList())
                    .retrievalTimeMs(retrievalTimeMs)
                    .generationTimeMs(0)
                    .totalTimeMs(retrievalTimeMs)
                    .build();
        }

        log.info("Building prompt...");
        String prompt = promptBuilderService.buildPrompt(request.getQuestion(), retrievedChunks, history);

        log.info("Calling LLM: {} / {}", llmService.getProviderName(), llmService.getModelName());
        long generationStartTime = System.currentTimeMillis();

        String answer;
        try {
            answer = llmService.generateAnswer(prompt).block();
        } catch (Exception e) {
            log.error("LLM generation failed", e);
            throw new RuntimeException("Failed to generate answer: " + e.getMessage(), e);
        }

        long generationTimeMs = System.currentTimeMillis() - generationStartTime;
        long totalTimeMs = System.currentTimeMillis() - startTime;

        if (request.getConversationId() != null) {
            conversationService.addMessageToConversation(
                    request.getConversationId(), user.getId(),
                    com.memoraai.conversation.entity.MessageRole.AI, answer);
        }

        log.info("Total request time: {}ms (retrieval={}ms, generation={}ms)",
                totalTimeMs, retrievalTimeMs, generationTimeMs);

        return ChatAskResponse.builder()
                .answer(answer)
                .provider(llmService.getProviderName())
                .model(llmService.getModelName())
                .sources(retrievedChunks)
                .retrievalTimeMs(retrievalTimeMs)
                .generationTimeMs(generationTimeMs)
                .totalTimeMs(totalTimeMs)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layer 2: Knowledge Graph query expansion
    //
    // If the query mentions concept names from the document's KG, we expand
    // it with 1-hop neighbour names so the embedding captures a richer
    // semantic neighbourhood — improving recall for related chunks.
    // ─────────────────────────────────────────────────────────────────────────
    private String expandQueryWithKG(String query, UUID documentId) {
        if (documentId == null) return query;

        List<Concept> concepts = conceptRepository.findByDocumentId(documentId);
        if (concepts.isEmpty()) return query;

        String lowerQuery = query.toLowerCase();
        List<UUID> matchedIds = new ArrayList<>();

        for (Concept c : concepts) {
            if (lowerQuery.contains(c.getNormalizedName())) {
                matchedIds.add(c.getId());
            }
        }

        Set<String> expansion = new LinkedHashSet<>();

        if (matchedIds.isEmpty()) {
            // No exact match — append the top-5 most important concepts as context hints
            concepts.stream()
                    .sorted(Comparator.comparingDouble(Concept::getImportanceScore).reversed())
                    .limit(5)
                    .map(Concept::getName)
                    .forEach(expansion::add);
        } else {
            // Matched concepts: walk 1-hop in the KG and collect neighbour names
            for (UUID cid : matchedIds) {
                List<ConceptRelationship> neighbours =
                        conceptRelationshipRepository.findNeighborsByConceptId(cid);
                for (ConceptRelationship rel : neighbours) {
                    Concept other = rel.getSourceConcept().getId().equals(cid)
                            ? rel.getTargetConcept()
                            : rel.getSourceConcept();
                    expansion.add(other.getName());
                }
            }
        }

        if (expansion.isEmpty()) return query;

        String ctx = expansion.stream().limit(8).collect(Collectors.joining(", "));
        return query + " [context: " + ctx + "]";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layer 3: ANME memory-weighted re-ranking
    //
    // After cosine retrieval, each chunk's score is multiplied by a factor
    // derived from the user's learning state for concepts it contains:
    //
    //   mastery 0.30–0.70  →  ×1.30  (active learning zone — most valuable)
    //   mastery > 0.70     →  ×0.85  (already known — slight demotion)
    //   mastery < 0.30     →  ×1.00  (not yet encountered — neutral)
    //   no memory state    →  ×1.00  (neutral)
    // ─────────────────────────────────────────────────────────────────────────
    private List<SearchResultItem> reRankWithANME(
            List<SearchResultItem> results, UUID userId, UUID documentId) {

        List<UserMemoryState> states =
                anmeMemoryService.getMemoryStatesByDocument(userId, documentId);
        if (states.isEmpty()) return results;

        Map<String, Double> conceptMultiplier = new HashMap<>();
        for (UserMemoryState s : states) {
            double m = s.getMemoryScore();
            double mult;
            if (m >= 0.30 && m <= 0.70) {
                mult = 1.30; // learning zone
            } else if (m > 0.70) {
                mult = 0.85; // mastered
            } else {
                mult = 1.00; // unfamiliar
            }
            conceptMultiplier.put(s.getConcept().getNormalizedName(), mult);
        }

        for (SearchResultItem item : results) {
            String text = item.getText().toLowerCase();
            double best = 1.0;
            for (Map.Entry<String, Double> e : conceptMultiplier.entrySet()) {
                if (text.contains(e.getKey())) {
                    best = Math.max(best, e.getValue());
                }
            }
            item.setScore(item.getScore() * best);
        }

        results.sort(Comparator.comparingDouble(SearchResultItem::getScore).reversed());
        return results;
    }
}
