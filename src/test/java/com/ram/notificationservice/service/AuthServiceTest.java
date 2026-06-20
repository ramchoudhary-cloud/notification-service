package com.ram.notificationservice.service;

import com.ram.notificationservice.dto.AuthRequest;
import com.ram.notificationservice.dto.AuthResponse;
import com.ram.notificationservice.exception.UsernameAlreadyExistsException;
import com.ram.notificationservice.model.Role;
import com.ram.notificationservice.model.User;
import com.ram.notificationservice.repository.UserRepository;
import com.ram.notificationservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndReturnToken() {
        AuthRequest request = new AuthRequest();
        request.setUsername("ram");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(1L).username("ram")
                .password("encoded").role(Role.USER).build();

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getAuthorities()).thenReturn(List.of());

        when(userRepository.existsByUsername("ram")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername("ram")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getUsername()).isEqualTo("ram");
        verify(userRepository).save(any());
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyExists() {
        AuthRequest request = new AuthRequest();
        request.setUsername("ram");
        request.setPassword("pass");

        when(userRepository.existsByUsername("ram")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("ram");

        verify(userRepository, never()).save(any());
    }
}
