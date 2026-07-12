package com.memoraai.auth.service;

import com.memoraai.auth.dto.AuthResponse;
import com.memoraai.auth.dto.RegisterRequest;
import com.memoraai.auth.util.JwtUtil;
import com.memoraai.common.exception.DuplicateEmailException;
import com.memoraai.profile.repository.UserProfileRepository;
import com.memoraai.user.entity.User;
import com.memoraai.user.mapper.UserMapper;
import com.memoraai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .build();
    }

    @Test
    void register_ShouldThrowExceptionIfEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> authenticationService.register(registerRequest));
    }

    @Test
    void register_ShouldSaveUserAndProfileAndReturnToken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtUtil.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        AuthResponse response = authenticationService.register(registerRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any());
    }
}
