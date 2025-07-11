package com.shopper.backend.service;

import com.shopper.backend.dto.AuthResponseDto;
import com.shopper.backend.dto.LoginDto;
import com.shopper.backend.dto.RegisterDto;
import com.shopper.backend.dto.UserDto;
import com.shopper.backend.entity.User;
import com.shopper.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponseDto register(RegisterDto registerDto) {
        // Check if user already exists
        if (userService.existsByUsername(registerDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userService.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setRole(User.UserRole.USER);

        User savedUser = userService.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(
            savedUser.getUsername(),
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getRole().name()
        );

        UserDto userDto = UserDto.fromUser(savedUser);
        return new AuthResponseDto(userDto, token);
    }

    public AuthResponseDto login(LoginDto loginDto) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(),
                loginDto.getPassword()
            )
        );

        // Get user details
        User user = userService.findByUsername(loginDto.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
            user.getUsername(),
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );

        UserDto userDto = UserDto.fromUser(user);
        return new AuthResponseDto(userDto, token);
    }
}