package com.memoraai.chunking.controller;

import com.memoraai.chunking.dto.ChunkResponse;
import com.memoraai.chunking.dto.ChunkStatisticsResponse;
import com.memoraai.chunking.service.DocumentChunkingService;
import com.memoraai.document.entity.Document;
import com.memoraai.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.memoraai.user.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents/{id}/chunks")
@RequiredArgsConstructor
public class DocumentChunkController {

    private final DocumentChunkingService documentChunkingService;
    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<ChunkResponse>> getChunks(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        
        Document document = documentService.getDocumentById(id, user);
        
        List<ChunkResponse> chunks = documentChunkingService.getChunksForDocument(document.getId());
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/statistics")
    public ResponseEntity<ChunkStatisticsResponse> getChunkStatistics(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        
        Document document = documentService.getDocumentById(id, user);
        
        ChunkStatisticsResponse stats = documentChunkingService.getChunkStatistics(document.getId());
        return ResponseEntity.ok(stats);
    }
}
