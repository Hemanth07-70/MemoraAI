package com.memoraai.concept.controller;

import com.memoraai.anme.dto.ConceptDto;
import com.memoraai.anme.mapper.ANMEMapper;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Concepts", description = "Concept Extraction and Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ConceptController {

    private final ConceptRepository conceptRepository;
    private final ANMEMapper anmeMapper;

    @Operation(summary = "Get extracted concepts for a document")
    @GetMapping("/documents/{documentId}/concepts")
    public ResponseEntity<ApiResponse<List<ConceptDto>>> getDocumentConcepts(@PathVariable UUID documentId) {
        List<ConceptDto> concepts = conceptRepository.findByDocumentId(documentId)
                .stream()
                .map(anmeMapper::conceptToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Concepts retrieved successfully", concepts));
    }
}
