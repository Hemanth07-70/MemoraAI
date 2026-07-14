package com.memoraai.documentintelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIntelligenceDto {
    private UUID id;
    private UUID documentId;
    private String executiveSummary;
    private List<String> skills;
    private List<String> technologies;
    private List<String> organizations;
    private List<String> education;
    private List<String> projects;
    private List<String> keywords;
}
