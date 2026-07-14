package com.memoraai.extracteddocument.service;

import com.memoraai.document.entity.Document;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import com.memoraai.extracteddocument.repository.ExtractedDocumentRepository;
import com.memoraai.processing.dto.AiProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractedDocumentService {

    private final ExtractedDocumentRepository extractedDocumentRepository;

    @Transactional
    public ExtractedDocument saveExtraction(Document document, AiProcessResponse response) {
        log.info("Saving extraction results for document {}", document.getId());
        
        if (response.getText() == null) {
            log.warn("Extracted text is null for document {}", document.getId());
        }

        ExtractedDocument extractedDocument = ExtractedDocument.builder()
                .document(document)
                .extractedText(response.getText() != null ? response.getText() : "")
                .pageCount(response.getPageCount())
                .wordCount(response.getWordCount())
                .characterCount(response.getCharacterCount())
                .build();

        ExtractedDocument saved = extractedDocumentRepository.save(extractedDocument);
        log.info("Successfully saved ExtractedDocument {} for document {}", saved.getId(), document.getId());
        return saved;
    }
}
