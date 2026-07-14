package com.memoraai.embedding.controller;

import com.memoraai.common.exception.ResourceNotFoundException;
import com.memoraai.document.entity.Document;
import com.memoraai.document.service.DocumentService;
import com.memoraai.embedding.dto.EmbeddingDetailResponse;
import com.memoraai.embedding.dto.EmbeddingMetadataResponse;
import com.memoraai.embedding.entity.DocumentEmbedding;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Embeddings", description = "Manage document chunk embeddings")
public class DocumentEmbeddingController {

    private final EmbeddingService embeddingService;
    private final DocumentService documentService;

    @GetMapping("/documents/{documentId}/embeddings")
    @Operation(summary = "Get embeddings metadata for a document", description = "Returns metadata for all chunk embeddings of a document without the large vector array")
    public ResponseEntity<List<EmbeddingMetadataResponse>> getDocumentEmbeddings(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {
        
        // Validation for ownership
        Document document = documentService.getDocumentById(documentId, user);
        
        List<DocumentEmbedding> embeddings = embeddingService.getEmbeddingsForDocument(document.getId());
        
        List<EmbeddingMetadataResponse> response = embeddings.stream()
                .map(emb -> EmbeddingMetadataResponse.builder()
                        .id(emb.getId())
                        .chunkId(emb.getChunk().getId())
                        .dimension(emb.getDimension())
                        .modelName(emb.getModelName())
                        .generationTimeMs(emb.getGenerationTimeMs())
                        .build())
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chunks/{chunkId}/embedding")
    @Operation(summary = "Get embedding for a specific chunk", description = "Returns the full embedding detail including the vector array")
    public ResponseEntity<EmbeddingDetailResponse> getChunkEmbedding(
            @PathVariable UUID chunkId,
            @AuthenticationPrincipal User user) {
        
        DocumentEmbedding embedding = embeddingService.getEmbeddingForChunk(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Embedding not found for chunk " + chunkId));
                
        // Validation for ownership (chunk -> extracted doc -> document -> owner)
        Document document = documentService.getDocumentById(embedding.getChunk().getExtractedDocument().getDocument().getId(), user);
        
        EmbeddingDetailResponse response = EmbeddingDetailResponse.builder()
                .id(embedding.getId())
                .chunkId(embedding.getChunk().getId())
                .dimension(embedding.getDimension())
                .modelName(embedding.getModelName())
                .generationTimeMs(embedding.getGenerationTimeMs())
                .embeddingJson(embedding.getEmbeddingJson())
                .build();
                
        return ResponseEntity.ok(response);
    }
}
