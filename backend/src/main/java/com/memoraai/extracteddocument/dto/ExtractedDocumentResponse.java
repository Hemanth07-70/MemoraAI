package com.memoraai.extracteddocument.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDocumentResponse {
    private UUID id;
    private UUID documentId;
    private Integer pageCount;
    private Integer wordCount;
    private Integer characterCount;
    private LocalDateTime extractedAt;
}
