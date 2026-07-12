package com.memoraai.processing.controller;

import com.memoraai.common.response.ApiResponse;
import com.memoraai.processing.dto.ProcessingJobResponse;
import com.memoraai.processing.mapper.ProcessingJobMapper;
import com.memoraai.processing.service.ProcessingJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/processing/jobs")
@RequiredArgsConstructor
@Tag(name = "Processing Job", description = "Background Processing Job APIs")
public class ProcessingJobController {

    private final ProcessingJobService processingJobService;
    private final ProcessingJobMapper processingJobMapper;

    @GetMapping("/{id}")
    @Operation(summary = "Get processing job by ID", description = "Fetches the status and progress of a processing job")
    public ResponseEntity<ApiResponse<ProcessingJobResponse>> getJobById(@PathVariable UUID id) {
        ProcessingJobResponse response = processingJobMapper.toResponse(processingJobService.getJobById(id));
        return ResponseEntity.ok(ApiResponse.success("Job fetched successfully", response));
    }
}
