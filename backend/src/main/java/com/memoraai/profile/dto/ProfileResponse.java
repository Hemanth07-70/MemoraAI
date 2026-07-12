package com.memoraai.profile.dto;

import com.memoraai.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private UUID id;
    private UserResponse user;
    private String profilePicture;
    private String bio;
    private String preferredLanguage;
    private String timezone;
    private String theme;
}
