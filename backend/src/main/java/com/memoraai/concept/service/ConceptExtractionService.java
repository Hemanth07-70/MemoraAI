package com.memoraai.concept.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.chat.service.LLMService;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.concept.dto.ConceptExtractionResult;
import com.memoraai.document.entity.Document;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.repository.DocumentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptExtractionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentEmbeddingRepository documentEmbeddingRepository;
    private final ConceptRepository conceptRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void extractConcepts(UUID documentId) {
        log.info("Starting concept extraction for document {}", documentId);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        List<DocumentChunk> chunks = documentChunkRepository.findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(documentId);
        if (chunks.isEmpty()) {
            log.warn("No chunks found for document {}", documentId);
            return;
        }

        log.info("Processing {} chunks for concept extraction in parallel batches", chunks.size());

        // Build batches up front so index is available when collecting results
        List<List<DocumentChunk>> batches = new ArrayList<>();
        List<String> batchTexts = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i += 5) {
            int end = Math.min(chunks.size(), i + 5);
            List<DocumentChunk> batch = new ArrayList<>(chunks.subList(i, end));
            StringBuilder sb = new StringBuilder();
            for (DocumentChunk chunk : batch) sb.append(chunk.getChunkText()).append("\n\n");
            batches.add(batch);
            batchTexts.add(sb.toString());
        }

        // Fire all Nemotron calls in parallel (cap at 4 concurrent to respect rate limits)
        int parallelism = Math.min(4, batches.size());
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        List<Future<List<ConceptExtractionResult>>> futures = new ArrayList<>();
        for (String text : batchTexts) {
            futures.add(executor.submit(() -> extractFromChunk(text)));
        }
        executor.shutdown();

        Map<String, AggregatedConcept> aggregatedConcepts = new HashMap<>();
        for (int i = 0; i < futures.size(); i++) {
            List<ConceptExtractionResult> extracted;
            try {
                extracted = futures.get(i).get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Batch {} extraction timed out or failed: {}", i, e.getMessage());
                extracted = Collections.emptyList();
            }

            List<DocumentChunk> batch = batches.get(i);
            for (ConceptExtractionResult res : extracted) {
                if (res.getName() == null || res.getName().isBlank()) continue;
                String normalizedName = res.getName().trim().toLowerCase();
                if (normalizedName.length() < 3 || normalizedName.split("\\s+").length > 5) continue;

                AggregatedConcept agg = aggregatedConcepts.computeIfAbsent(normalizedName, k -> new AggregatedConcept(res.getName(), batch.get(0)));
                agg.frequency++;
                agg.llmImportanceSum += (res.getImportance() != null ? res.getImportance() : 0.0);
                agg.llmDifficultySum += (res.getDifficulty() != null ? res.getDifficulty() : 0.0);
                if (res.getDescription() != null && res.getDescription().length() > agg.description.length()) {
                    agg.description = res.getDescription();
                }
            }
        }

        log.info("LLM extraction complete. Found {} unique concepts before filtering.", aggregatedConcepts.size());

        List<Concept> conceptsToSave = new ArrayList<>();
        int totalChunks = chunks.size();

        for (AggregatedConcept agg : aggregatedConcepts.values()) {
            // Retrieve embedding for the chunk where this concept first/most appeared
            DocumentEmbedding embedding = documentEmbeddingRepository.findByChunkId(agg.firstSeenChunk.getId())
                    .orElse(null);

            double avgLlmImportance = agg.llmImportanceSum / agg.frequency;
            double avgLlmDifficulty = agg.llmDifficultySum / agg.frequency;
            
            double frequencyScore = Math.min(1.0, (double) agg.frequency / Math.max(1, totalChunks / 5.0)); // Cap frequency impact
            
            boolean inTitle = false;
            if (document.getOriginalFileName() != null && document.getOriginalFileName().toLowerCase().contains(agg.normalizedName)) {
                inTitle = true;
            }
            
            double headingBoost = inTitle ? 1.0 : (agg.firstSeenChunk.getChunkIndex() == 0 ? 0.5 : 0.0);
            double embeddingCentrality = 0.5; // Placeholder for actual cosine centrality
            
            double finalImportance = (0.40 * frequencyScore) + (0.30 * headingBoost) + (0.20 * avgLlmImportance) + (0.10 * embeddingCentrality);
            finalImportance = Math.min(1.0, Math.max(0.0, finalImportance));

            double lengthScore = Math.min(1.0, agg.description.split("\\s+").length / 30.0);
            double connectivity = Math.min(1.0, agg.frequency / 10.0);
            double finalDifficulty = (0.40 * avgLlmDifficulty) + (0.30 * connectivity) + (0.20 * lengthScore) + (0.10 * 0.0);
            finalDifficulty = Math.min(1.0, Math.max(0.0, finalDifficulty));

            Optional<Concept> existingConceptOpt = conceptRepository.findByDocumentIdAndNormalizedName(documentId, agg.normalizedName);

            if (existingConceptOpt.isPresent()) {
                Concept existingConcept = existingConceptOpt.get();
                existingConcept.setFrequency(existingConcept.getFrequency() + agg.frequency);
                
                // Recalculate using the new frequency, but keeping the bounds and existing logic
                double newFrequencyScore = Math.min(1.0, (double) existingConcept.getFrequency() / Math.max(1, totalChunks / 5.0));
                
                // Simple moving average for LLM scores based on frequency could be complex, we'll just blend the old score with the new extraction
                // Since this is idempotency across jobs, we just average the new findings into the final
                double newImportance = (existingConcept.getImportanceScore() + finalImportance) / 2.0;
                newImportance = Math.min(1.0, Math.max(0.0, newImportance));
                
                double newDifficulty = (existingConcept.getDifficultyScore() + finalDifficulty) / 2.0;
                newDifficulty = Math.min(1.0, Math.max(0.0, newDifficulty));
                
                existingConcept.setImportanceScore(newImportance);
                existingConcept.setDifficultyScore(newDifficulty);
                
                if (agg.description.length() > existingConcept.getDescription().length()) {
                    existingConcept.setDescription(agg.description);
                }
                
                conceptsToSave.add(existingConcept);
            } else {
                Concept concept = Concept.builder()
                        .document(document)
                        .name(agg.originalName)
                        .normalizedName(agg.normalizedName)
                        .description(agg.description)
                        .importanceScore(finalImportance)
                        .difficultyScore(finalDifficulty)
                        .embedding(embedding)
                        .frequency(agg.frequency)
                        .build();
                        
                conceptsToSave.add(concept);
            }
        }

        // Keep top 30 most important
        List<Concept> topConcepts = conceptsToSave.stream()
                .sorted((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()))
                .limit(30)
                .collect(Collectors.toList());

        conceptRepository.saveAll(topConcepts);
        
        log.info("Persisted {} concepts for document {}", topConcepts.size(), documentId);
    }

    private List<ConceptExtractionResult> extractFromChunk(String text) {
        String prompt = "You are an educational knowledge extraction engine.\n" +
                "Extract only the important learning concepts from this text.\n" +
                "For each concept return:\n" +
                "- name\n" +
                "- description\n" +
                "- importance (0.0-1.0)\n" +
                "- difficulty (0.0-1.0)\n\n" +
                "Return ONLY valid JSON. Do not return markdown. Do not return any other text.\n" +
                "Example:\n" +
                "[\n" +
                "  {\n" +
                "    \"name\":\"Transformer\",\n" +
                "    \"description\":\"Neural network architecture\",\n" +
                "    \"importance\":0.95,\n" +
                "    \"difficulty\":0.84\n" +
                "  }\n" +
                "]\n\n" +
                "Text: " + text;

        try {
            String json = llmService.generateAnswer(prompt).block();
            if (json == null) return Collections.emptyList();
            
            // Clean markdown block if model ignored instructions
            json = json.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            } else if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            return objectMapper.readValue(json, new TypeReference<List<ConceptExtractionResult>>() {});
        } catch (Exception e) {
            log.error("Failed to parse JSON from LLM: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static class AggregatedConcept {
        String normalizedName;
        String originalName;
        String description = "";
        int frequency = 0;
        double llmImportanceSum = 0.0;
        double llmDifficultySum = 0.0;
        DocumentChunk firstSeenChunk;

        AggregatedConcept(String originalName, DocumentChunk firstSeenChunk) {
            this.originalName = originalName;
            this.normalizedName = originalName.trim().toLowerCase();
            this.firstSeenChunk = firstSeenChunk;
        }
    }
}
