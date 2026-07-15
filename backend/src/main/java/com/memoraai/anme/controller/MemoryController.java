package com.memoraai.anme.controller;

import com.memoraai.anme.dto.UserMemoryStateDto;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.mapper.ANMEMapper;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.common.response.ApiResponse;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@Tag(name = "Memory Engine", description = "Adaptive Neural Memory Engine APIs")
@SecurityRequirement(name = "bearerAuth")
public class MemoryController {

    private final ANMEMemoryService memoryService;
    private final ANMEMapper anmeMapper;

    @Operation(summary = "Get all memory states for the current user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<UserMemoryStateDto>>> getUserMemoryStates(
            @AuthenticationPrincipal User user) {

        List<UserMemoryState> states = memoryService.getUserMemoryStates(user.getId());
        List<UserMemoryStateDto> dtos = states.stream()
                .map(anmeMapper::userMemoryStateToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Memory states retrieved successfully", dtos));
    }

    @Operation(summary = "Get memory states for a specific document")
    @GetMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<List<UserMemoryStateDto>>> getMemoryStatesForDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {

        List<UserMemoryState> states = memoryService.getMemoryStatesByDocument(user.getId(), documentId);
        List<UserMemoryStateDto> dtos = states.stream()
                .map(anmeMapper::userMemoryStateToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Document memory states retrieved successfully", dtos));
    }
}
