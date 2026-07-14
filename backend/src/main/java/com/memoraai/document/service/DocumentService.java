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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final DocumentValidator documentValidator;
    private final ProcessingJobService processingJobService;

    @Transactional
    public Document uploadDocument(MultipartFile file, User owner) {
        documentValidator.validateUpload(file);

        String originalFilename = file.getOriginalFilename();
        String extension = documentValidator.getExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + "." + extension;

        String storagePath = storageService.store(file, uniqueFileName);

        Document document = Document.builder()
                .owner(owner)
                .fileName(uniqueFileName)
                .originalFileName(originalFilename)
                .mimeType(file.getContentType())
                .extension(extension)
                .size(file.getSize())
                .storagePath(storagePath)
                .status(DocumentStatus.UPLOADED)
                .isDeleted(false)
                .build();

        document = documentRepository.save(document);

        // Create initial processing jobs (e.g., OCR)
        processingJobService.createJob(document, JobType.OCR);

        return document;
    }

    @Transactional(readOnly = true)
    public List<Document> getUserDocuments(User owner) {
        return documentRepository.findByOwnerAndIsDeletedFalse(owner);
    }

    @Transactional(readOnly = true)
    public Document getDocumentById(UUID id, User owner) {
        return documentRepository.findByIdAndOwnerAndIsDeletedFalse(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found or access denied"));
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(UUID id, User owner) {
        Document document = getDocumentById(id, owner);
        return storageService.loadAsResource(document.getStoragePath());
    }

    @Transactional
    public void deleteDocument(UUID id, User owner) {
        Document document = getDocumentById(id, owner);
        document.setIsDeleted(true);
        documentRepository.save(document);
        // Note: Soft delete only. The physical file is left intact per requirements.
    }
}
