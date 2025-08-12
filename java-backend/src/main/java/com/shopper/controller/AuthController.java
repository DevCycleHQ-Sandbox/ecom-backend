package com.shopper.controller;

import com.shopper.dto.AuthResponseDto;
import com.shopper.dto.LoginDto;
import com.shopper.dto.RegisterDto;
import com.shopper.entity.User;
import com.shopper.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterDto registerDto) {
        try {
            AuthResponseDto response = userService.register(registerDto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
        try {
            AuthResponseDto response = userService.login(loginDto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/verify")
    @Operation(summary = "Verify JWT token")
    public ResponseEntity<?> verify() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            
            // Handle both UserDetails and User entity cases
            User user = null;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                user = userService.findByUsername(username);
            }
            
            if (user != null) {
                return ResponseEntity.ok(new UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole().getValue()));
            }
        }
        
        return ResponseEntity.status(401).build();
    }
    
    // Helper classes for response
    public static class VerifyResponse {
        private final String message;
        private final UserInfo user;
        
        public VerifyResponse(String message, UserInfo user) {
            this.message = message;
            this.user = user;
        }
        
        public String getMessage() { return message; }
        public UserInfo getUser() { return user; }
    }
    
    public static class UserInfo {
        private final UUID id;
        private final String username;
        private final String email;
        private final String role;
        
        public UserInfo(UUID id, String username, String email, String role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
        }
        
        public UUID getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
    
    @PostMapping("/init-admin")
    @Operation(summary = "Initialize admin user (for setup purposes)")
    public ResponseEntity<?> initializeAdmin() {
        try {
            userService.initializeAdminUser();
            return ResponseEntity.ok().body("{\"message\": \"Admin user initialized successfully\"}");
        } catch (Exception e) {
            log.error("Admin initialization failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}