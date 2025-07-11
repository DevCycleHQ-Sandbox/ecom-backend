package com.shopper.service;

import com.shopper.dto.AuthResponseDto;
import com.shopper.dto.LoginDto;
import com.shopper.dto.RegisterDto;
import com.shopper.entity.User;
import com.shopper.repository.UserRepository;
import com.shopper.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    public AuthResponseDto register(RegisterDto registerDto) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        User user = User.builder()
                .username(registerDto.getUsername())
                .email(registerDto.getEmail())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role(User.Role.USER)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser);
        
        return new AuthResponseDto(token, savedUser);
    }
    
    public AuthResponseDto login(LoginDto loginDto) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );
        
        // Get user details
        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginDto.getUsername()));
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user);
        
        return new AuthResponseDto(token, user);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public long countUsers() {
        return userRepository.countUsers();
    }
    
    public long countAdmins() {
        return userRepository.countAdmins();
    }
}