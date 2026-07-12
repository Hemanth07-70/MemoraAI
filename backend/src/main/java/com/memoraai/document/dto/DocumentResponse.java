package com.memoraai.document.dto;

import com.memoraai.document.entity.DocumentStatus;
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
public class DocumentResponse {
    private UUID id;
    private String originalFileName;
    private String mimeType;
    private String extension;
    private Long size;
    private DocumentStatus status;
    private String downloadUrl;
    private LocalDateTime uploadedAt;
}
