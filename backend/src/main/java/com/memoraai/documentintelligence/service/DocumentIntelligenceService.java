package com.memoraai.documentintelligence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.chat.service.LLMService;
import com.memoraai.document.entity.Document;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.documentintelligence.dto.DocumentIntelligenceDto;
import com.memoraai.documentintelligence.entity.DocumentIntelligence;
import com.memoraai.documentintelligence.repository.DocumentIntelligenceRepository;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import com.memoraai.extracteddocument.repository.ExtractedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIntelligenceService {

    private final DocumentIntelligenceRepository intelligenceRepository;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final DocumentRepository documentRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    private static final String INTELLIGENCE_PROMPT_TEMPLATE = """
            You are an expert AI document analyzer. Your task is to extract structured intelligence from the following document text.
            
            Return ONLY a valid JSON object with the following schema. Do NOT include Markdown formatting like ```json or any other text before or after the JSON.
            {
              "executiveSummary": "A concise paragraph summarizing the document",
              "skills": ["Skill 1", "Skill 2"],
              "technologies": ["Tech 1", "Tech 2"],
              "organizations": ["Org 1", "Org 2"],
              "education": ["Edu 1", "Edu 2"],
              "projects": ["Project 1", "Project 2"],
              "keywords": ["Keyword 1", "Keyword 2"]
            }
            
            Document Text:
            %s
            """;

    @Transactional
    public void generateIntelligence(UUID documentId) {
        log.info("Generating intelligence for document {}", documentId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        ExtractedDocument extracted = extractedDocumentRepository.findByDocument(document)
                .orElseThrow(() -> new RuntimeException("Extracted document not found"));

        String text = extracted.getExtractedText();
        // Truncate to avoid context window limits (~50,000 characters is a safe bet for most large models)
        if (text.length() > 20000) {
            text = text.substring(0, 20000);
            log.warn("Truncated extracted text for document {} to 20000 characters for intelligence generation.", documentId);
        }

        String prompt = String.format(INTELLIGENCE_PROMPT_TEMPLATE, text);

        try {
            String jsonResponse = llmService.generateAnswer(prompt).block();
            
            // Cleanup response in case the LLM returned markdown code blocks
            if (jsonResponse != null) {
                jsonResponse = jsonResponse.trim();
                if (jsonResponse.startsWith("```json")) {
                    jsonResponse = jsonResponse.substring(7);
                } else if (jsonResponse.startsWith("```")) {
                    jsonResponse = jsonResponse.substring(3);
                }
                if (jsonResponse.endsWith("```")) {
                    jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
                }
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            DocumentIntelligence intelligence = DocumentIntelligence.builder()
                    .document(document)
                    .executiveSummary(rootNode.path("executiveSummary").asText(null))
                    .skills(extractList(rootNode, "skills"))
                    .technologies(extractList(rootNode, "technologies"))
                    .organizations(extractList(rootNode, "organizations"))
                    .education(extractList(rootNode, "education"))
                    .projects(extractList(rootNode, "projects"))
                    .keywords(extractList(rootNode, "keywords"))
                    .build();

            intelligenceRepository.save(intelligence);
            log.info("Successfully generated and saved intelligence for document {}", documentId);
            
        } catch (Exception e) {
            log.error("Failed to generate intelligence for document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate document intelligence", e);
        }
    }

    @Transactional(readOnly = true)
    public DocumentIntelligenceDto getIntelligence(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
                
        if (!document.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        DocumentIntelligence intelligence = intelligenceRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Intelligence not generated for this document yet"));

        return DocumentIntelligenceDto.builder()
                .id(intelligence.getId())
                .documentId(intelligence.getDocument().getId())
                .executiveSummary(intelligence.getExecutiveSummary())
                .skills(intelligence.getSkills())
                .technologies(intelligence.getTechnologies())
                .organizations(intelligence.getOrganizations())
                .education(intelligence.getEducation())
                .projects(intelligence.getProjects())
                .keywords(intelligence.getKeywords())
                .build();
    }

    private List<String> extractList(JsonNode rootNode, String fieldName) {
        List<String> list = new ArrayList<>();
        JsonNode arrayNode = rootNode.path(fieldName);
        if (arrayNode.isArray()) {
            for (Iterator<JsonNode> it = arrayNode.elements(); it.hasNext(); ) {
                JsonNode node = it.next();
                list.add(node.asText());
            }
        }
        return list;
    }
}
