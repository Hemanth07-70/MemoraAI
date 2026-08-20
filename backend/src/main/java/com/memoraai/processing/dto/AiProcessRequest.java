package com.memoraai.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProcessRequest {
    private UUID jobId;
    private UUID documentId;
    private String jobType;
    private String filePath;
    private String fileContent; // base64-encoded file bytes for cloud deployment
}
