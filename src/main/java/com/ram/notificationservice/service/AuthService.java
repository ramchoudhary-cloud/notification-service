package com.ram.notificationservice.service;

import com.ram.notificationservice.dto.AuthRequest;
import com.ram.notificationservice.dto.AuthResponse;
import com.ram.notificationservice.exception.UsernameAlreadyExistsException;
import com.ram.notificationservice.model.Role;
import com.ram.notificationservice.model.User;
import com.ram.notificationservice.repository.UserRepository;
import com.ram.notificationservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", request.getUsername());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(AuthRequest request) {
        // this throws BadCredentialsException if wrong - Spring handles it
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}
