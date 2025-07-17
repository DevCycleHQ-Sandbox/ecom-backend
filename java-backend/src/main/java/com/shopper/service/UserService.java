package com.shopper.service;

import com.shopper.dto.AuthResponseDto;
import com.shopper.dto.LoginDto;
import com.shopper.dto.RegisterDto;
import com.shopper.entity.User;
import com.shopper.repository.UserRepository;
import com.shopper.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase())
                ))
                .build();
    }

    public AuthResponseDto register(RegisterDto registerDto) {
        // Check if username already exists
        if (existsByUsername(registerDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(registerDto.getUsername())
                .email(registerDto.getEmail())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        UserDetails userDetails = loadUserByUsername(savedUser.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        
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
        UserDetails userDetails = loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        
        return new AuthResponseDto(token, user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public Optional<User> findByUsernameOptional(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public User createUser(String username, String email, String password, User.Role role) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }

    public User updateUser(UUID userId, String username, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (username != null && !username.equals(user.getUsername())) {
            if (existsByUsername(username)) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
            user.setUsername(username);
        }

        if (email != null && !email.equals(user.getEmail())) {
            if (existsByEmail(email)) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }
            user.setEmail(email);
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User changePassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}