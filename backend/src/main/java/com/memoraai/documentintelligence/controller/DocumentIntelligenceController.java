package com.memoraai.documentintelligence.controller;

import com.memoraai.common.response.ApiResponse;
import com.memoraai.documentintelligence.dto.DocumentIntelligenceDto;
import com.memoraai.documentintelligence.service.DocumentIntelligenceService;
import com.memoraai.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Intelligence", description = "Endpoints for fetching document summaries and intelligence")
public class DocumentIntelligenceController {

    private final DocumentIntelligenceService intelligenceService;

    @GetMapping("/{documentId}/intelligence")
    @Operation(summary = "Get document intelligence", description = "Retrieves AI-generated intelligence (summary, skills, etc.) for a document")
    public ResponseEntity<ApiResponse<DocumentIntelligenceDto>> getIntelligence(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {
        DocumentIntelligenceDto dto = intelligenceService.getIntelligence(documentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Document intelligence retrieved successfully", dto));
    }
}
