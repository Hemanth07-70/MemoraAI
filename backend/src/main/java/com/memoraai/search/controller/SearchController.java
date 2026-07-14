package com.memoraai.search.controller;

import com.memoraai.search.dto.SearchRequest;
import com.memoraai.search.dto.SearchResponse;
import com.memoraai.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Semantic Search", description = "Semantic search over processed documents")
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @Operation(summary = "Search documents semantically", description = "Returns the most relevant document chunks ranked by cosine similarity.")
    public ResponseEntity<SearchResponse> search(
            @Valid @RequestBody SearchRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.memoraai.user.entity.User user) {
        SearchResponse response = searchService.search(request, user);
        return ResponseEntity.ok(response);
    }
}
