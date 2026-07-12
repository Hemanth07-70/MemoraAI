package com.memoraai.document.controller;

import com.memoraai.common.response.ApiResponse;
import com.memoraai.document.dto.DocumentListResponse;
import com.memoraai.document.dto.DocumentResponse;
import com.memoraai.document.dto.UploadResponse;
import com.memoraai.document.entity.Document;
import com.memoraai.document.mapper.DocumentMapper;
import com.memoraai.document.service.DocumentService;
import com.memoraai.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "Document Management APIs")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document", description = "Uploads a new document (PDF, DOCX, PPTX, TXT, PNG, JPG, JPEG) up to 50MB")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        Document document = documentService.uploadDocument(file, user);
        DocumentResponse response = documentMapper.toResponse(document);
        UploadResponse uploadResponse = UploadResponse.builder()
                .document(response)
                .message("File uploaded successfully and queued for processing")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Upload successful", uploadResponse));
    }

    @GetMapping
    @Operation(summary = "List documents", description = "List all documents owned by the authenticated user")
    public ResponseEntity<ApiResponse<DocumentListResponse>> listDocuments(@AuthenticationPrincipal User user) {
        List<Document> documents = documentService.getUserDocuments(user);
        List<DocumentResponse> responseList = documentMapper.toResponseList(documents);
        DocumentListResponse listResponse = DocumentListResponse.builder()
                .documents(responseList)
                .totalCount(responseList.size())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Documents fetched successfully", listResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata", description = "Get metadata of a specific document owned by the user")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        Document document = documentService.getDocumentById(id, user);
        return ResponseEntity.ok(ApiResponse.success("Document metadata fetched successfully", documentMapper.toResponse(document)));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document", description = "Download the actual physical file")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        Document document = documentService.getDocumentById(id, user);
        Resource resource = documentService.downloadDocument(id, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document", description = "Soft delete a document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        documentService.deleteDocument(id, user);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
}
