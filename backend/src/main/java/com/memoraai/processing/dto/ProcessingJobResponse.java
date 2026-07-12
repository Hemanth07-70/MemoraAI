package com.memoraai.processing.dto;

import com.memoraai.processing.entity.JobStatus;
import com.memoraai.processing.entity.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessingJobResponse {
    private UUID id;
    private UUID documentId;
    private JobType jobType;
    private JobStatus status;
    private Integer progress;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
