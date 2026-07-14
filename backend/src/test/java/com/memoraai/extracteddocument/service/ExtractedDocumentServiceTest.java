package com.memoraai.extracteddocument.service;

import com.memoraai.document.entity.Document;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import com.memoraai.extracteddocument.repository.ExtractedDocumentRepository;
import com.memoraai.processing.dto.AiProcessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExtractedDocumentServiceTest {

    private ExtractedDocumentRepository extractedDocumentRepository;
    private ExtractedDocumentService extractedDocumentService;

    @BeforeEach
    void setUp() {
        extractedDocumentRepository = mock(ExtractedDocumentRepository.class);
        extractedDocumentService = new ExtractedDocumentService(extractedDocumentRepository);
    }

    @Test
    void testSaveExtraction() {
        Document document = Document.builder().id(UUID.randomUUID()).build();
        AiProcessResponse response = AiProcessResponse.builder()
                .success(true)
                .text("This is extracted text.")
                .pageCount(2)
                .wordCount(4)
                .characterCount(23)
                .build();

        when(extractedDocumentRepository.save(any(ExtractedDocument.class)))
                .thenAnswer(invocation -> {
                    ExtractedDocument saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        ExtractedDocument result = extractedDocumentService.saveExtraction(document, response);

        assertNotNull(result.getId());
        
        ArgumentCaptor<ExtractedDocument> captor = ArgumentCaptor.forClass(ExtractedDocument.class);
        verify(extractedDocumentRepository).save(captor.capture());
        
        ExtractedDocument savedDoc = captor.getValue();
        assertEquals(document, savedDoc.getDocument());
        assertEquals("This is extracted text.", savedDoc.getExtractedText());
        assertEquals(2, savedDoc.getPageCount());
        assertEquals(4, savedDoc.getWordCount());
        assertEquals(23, savedDoc.getCharacterCount());
    }
}
