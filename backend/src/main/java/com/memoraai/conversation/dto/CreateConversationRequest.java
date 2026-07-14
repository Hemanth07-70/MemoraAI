package com.memoraai.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    @NotBlank(message = "Title is required")
    private String title;
}
