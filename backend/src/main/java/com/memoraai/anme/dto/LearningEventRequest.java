package com.memoraai.anme.dto;

import com.memoraai.anme.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningEventRequest {
    private UUID conceptId;
    private EventType eventType;
    private Long duration;
    private Double confidence;
    private Double score;
    private String metadata;
}
