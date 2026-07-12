package com.memoraai.profile.controller;

import com.memoraai.common.response.ApiResponse;
import com.memoraai.profile.dto.ProfileResponse;
import com.memoraai.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User Profile Management APIs")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    @Operation(summary = "Get user profile", description = "Fetches the extended profile of the authenticated user")
    public ResponseEntity<ApiResponse<ProfileResponse>> getCurrentUserProfile(Authentication authentication) {
        ProfileResponse profile = userProfileService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", profile));
    }
}
