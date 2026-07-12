package com.memoraai.profile.service;

import com.memoraai.common.exception.ResourceNotFoundException;
import com.memoraai.profile.dto.ProfileResponse;
import com.memoraai.profile.entity.UserProfile;
import com.memoraai.profile.mapper.UserProfileMapper;
import com.memoraai.profile.repository.UserProfileRepository;
import com.memoraai.user.entity.User;
import com.memoraai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    public ProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + email));

        return userProfileMapper.toResponse(profile);
    }
}
