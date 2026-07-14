package com.memoraai.document.service;

import com.memoraai.common.exception.ResourceNotFoundException;
import com.memoraai.document.entity.Document;
import com.memoraai.document.entity.DocumentStatus;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.document.storage.StorageService;
import com.memoraai.document.validation.DocumentValidator;
import com.memoraai.processing.entity.JobType;
import com.memoraai.processing.service.ProcessingJobService;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentValidator documentValidator;

    @Mock
    private ProcessingJobService processingJobService;

    @InjectMocks
    private DocumentService documentService;

    private User testOwner;
    private Document testDocument;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        testOwner = User.builder().id(UUID.randomUUID()).email("test@test.com").build();
        documentId = UUID.randomUUID();
        testDocument = Document.builder()
                .id(documentId)
                .owner(testOwner)
                .fileName("uuid.pdf")
                .originalFileName("test.pdf")
                .mimeType("application/pdf")
                .extension("pdf")
                .size(1024L)
                .storagePath("/path/to/uuid.pdf")
                .status(DocumentStatus.UPLOADED)
                .isDeleted(false)
                .build();
    }

    @Test
    void uploadDocument_success() {
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        
        doNothing().when(documentValidator).validateUpload(mockFile);
        when(documentValidator.getExtension("test.pdf")).thenReturn("pdf");
        when(storageService.store(eq(mockFile), any(String.class))).thenReturn("/path/to/uuid.pdf");
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        
        Document result = documentService.uploadDocument(mockFile, testOwner);
        
        assertNotNull(result);
        assertEquals(documentId, result.getId());
        
        verify(documentRepository).save(any(Document.class));
        verify(processingJobService).createJob(testDocument, JobType.OCR);
        verify(processingJobService, never()).createJob(testDocument, JobType.EMBEDDING);
    }

    @Test
    void getUserDocuments_returnsList() {
        when(documentRepository.findByOwnerAndIsDeletedFalse(testOwner)).thenReturn(List.of(testDocument));
        
        List<Document> result = documentService.getUserDocuments(testOwner);
        
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getDocumentById_success() {
        when(documentRepository.findByIdAndOwnerAndIsDeletedFalse(documentId, testOwner))
                .thenReturn(Optional.of(testDocument));
        
        Document result = documentService.getDocumentById(documentId, testOwner);
        
        assertNotNull(result);
        assertEquals(documentId, result.getId());
    }

    @Test
    void getDocumentById_notFound() {
        when(documentRepository.findByIdAndOwnerAndIsDeletedFalse(documentId, testOwner))
                .thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocumentById(documentId, testOwner));
    }

    @Test
    void downloadDocument_success() {
        Resource mockResource = mock(Resource.class);
        when(documentRepository.findByIdAndOwnerAndIsDeletedFalse(documentId, testOwner))
                .thenReturn(Optional.of(testDocument));
        when(storageService.loadAsResource("/path/to/uuid.pdf")).thenReturn(mockResource);
        
        Resource result = documentService.downloadDocument(documentId, testOwner);
        
        assertNotNull(result);
    }

    @Test
    void deleteDocument_success() {
        when(documentRepository.findByIdAndOwnerAndIsDeletedFalse(documentId, testOwner))
                .thenReturn(Optional.of(testDocument));
        
        documentService.deleteDocument(documentId, testOwner);
        
        assertTrue(testDocument.getIsDeleted());
        verify(documentRepository).save(testDocument);
    }
}
