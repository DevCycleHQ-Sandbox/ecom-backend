package com.shopper;

import com.shopper.dto.AuthResponseDto;
import com.shopper.dto.LoginDto;
import com.shopper.dto.RegisterDto;
import com.shopper.entity.User;
import com.shopper.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple authentication tests to verify basic functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimpleAuthTest {

    @Autowired
    private UserService userService;

    @Test
    void testUserRegistration() {
        RegisterDto registerDto = new RegisterDto();
        registerDto.setUsername("testuser");
        registerDto.setEmail("testuser@example.com");
        registerDto.setPassword("password123");

        AuthResponseDto response = userService.register(registerDto);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        assertThat(response.getUser().getEmail()).isEqualTo("testuser@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("user");
    }

    @Test
    void testUserLogin() {
        // First register a user
        userService.createUser("logintest", "logintest@example.com", "password123", User.Role.USER);

        // Then try to login
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("logintest");
        loginDto.setPassword("password123");

        AuthResponseDto response = userService.login(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getUser().getUsername()).isEqualTo("logintest");
        assertThat(response.getUser().getRole()).isEqualTo("user");
    }

    @Test
    void testAdminUserCreation() {
        User adminUser = userService.createUser("admin", "admin@example.com", "admin123", User.Role.ADMIN);

        assertThat(adminUser).isNotNull();
        assertThat(adminUser.getUsername()).isEqualTo("admin");
        assertThat(adminUser.getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void testAdminUserLogin() {
        // Create admin user
        userService.createUser("admintest", "admintest@example.com", "admin123", User.Role.ADMIN);

        // Login as admin
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("admintest");
        loginDto.setPassword("admin123");

        AuthResponseDto response = userService.login(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getUser().getUsername()).isEqualTo("admintest");
        assertThat(response.getUser().getRole()).isEqualTo("admin");
    }
} 