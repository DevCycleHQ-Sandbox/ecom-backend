package com.shopper.integration;

import com.shopper.BaseIntegrationTest;
import com.shopper.dto.AuthResponseDto;
import com.shopper.dto.LoginDto;
import com.shopper.dto.RegisterDto;
import com.shopper.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for user authentication functionality
 * Tests both admin and regular user login/registration across different data sources
 */
@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should successfully register and login admin user")
    void testAdminUserRegistrationAndLogin() throws Exception {
        // Test admin registration
        RegisterDto adminRegisterDto = new RegisterDto();
        adminRegisterDto.setUsername("newadmin@test.com");
        adminRegisterDto.setEmail("newadmin@test.com");
        adminRegisterDto.setPassword("newadmin123");

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(adminRegisterDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("newadmin@test.com"))
                .andExpect(jsonPath("$.user.email").value("newadmin@test.com"))
                .andExpect(jsonPath("$.user.role").value("user")) // Default role is USER
                .andReturn();

        AuthResponseDto registerResponse = fromJson(
            registerResult.getResponse().getContentAsString(), 
            AuthResponseDto.class
        );
        assertThat(registerResponse.getAccessToken()).isNotEmpty();
        assertThat(registerResponse.getUser().getRole()).isEqualTo("user");

        // Test admin login
        LoginDto adminLoginDto = new LoginDto();
        adminLoginDto.setUsername("newadmin@test.com");
        adminLoginDto.setPassword("newadmin123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(adminLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("newadmin@test.com"))
                .andReturn();

        AuthResponseDto loginResponse = fromJson(
            loginResult.getResponse().getContentAsString(), 
            AuthResponseDto.class
        );
        assertThat(loginResponse.getAccessToken()).isNotEmpty();

        // Test JWT verification
        mockMvc.perform(get("/auth/verify")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"))
                .andExpect(jsonPath("$.user.username").value("newadmin@test.com"));
    }

    @Test
    @DisplayName("Should successfully login existing admin user")
    void testExistingAdminUserLogin() throws Exception {
        // Login with existing admin user created in base setup
        LoginDto adminLoginDto = new LoginDto();
        adminLoginDto.setUsername("admin@test.com");
        adminLoginDto.setPassword("admin123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(adminLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("admin@test.com"))
                .andExpect(jsonPath("$.user.role").value("admin"))
                .andReturn();

        AuthResponseDto loginResponse = fromJson(
            loginResult.getResponse().getContentAsString(), 
            AuthResponseDto.class
        );

        // Verify the JWT token works
        mockMvc.perform(get("/auth/verify")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"))
                .andExpect(jsonPath("$.user.username").value("admin@test.com"))
                .andExpect(jsonPath("$.user.role").value("admin"));

        // Store admin token for other tests
        adminJwtToken = loginResponse.getAccessToken();
    }

    @Test
    @DisplayName("Should successfully register and login regular user")
    void testRegularUserRegistrationAndLogin() throws Exception {
        // Test regular user registration
        RegisterDto userRegisterDto = new RegisterDto();
        userRegisterDto.setUsername("newuser@test.com");
        userRegisterDto.setEmail("newuser@test.com");
        userRegisterDto.setPassword("newuser123");

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(userRegisterDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("newuser@test.com"))
                .andExpect(jsonPath("$.user.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.user.role").value("user"))
                .andReturn();

        AuthResponseDto registerResponse = fromJson(
            registerResult.getResponse().getContentAsString(), 
            AuthResponseDto.class
        );
        assertThat(registerResponse.getAccessToken()).isNotEmpty();

        // Test regular user login
        LoginDto userLoginDto = new LoginDto();
        userLoginDto.setUsername("newuser@test.com");
        userLoginDto.setPassword("newuser123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(userLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("newuser@test.com"))
                .andExpect(jsonPath("$.user.role").value("user"))
                .andReturn();

        AuthResponseDto loginResponse = fromJson(
            loginResult.getResponse().getContentAsString(), 
            AuthResponseDto.class
        );

        // Test JWT verification for regular user
        mockMvc.perform(get("/auth/verify")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"))
                .andExpect(jsonPath("$.user.username").value("newuser@test.com"))
                .andExpect(jsonPath("$.user.role").value("user"));
    }

    @Test
    @DisplayName("Should successfully login existing regular user")
    void testExistingRegularUserLogin() throws Exception {
        // Login with existing regular user created in base setup
        LoginDto userLoginDto = new LoginDto();
        userLoginDto.setUsername("testuser@test.com");
        userLoginDto.setPassword("testpass123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(userLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("testuser@test.com"))
                .andExpect(jsonPath("$.user.role").value("user"))
                .andReturn();

        AuthResponseDto loginResponse = fromJson(
            loginResult.getResponse().getContentAsString(), 
            AuthResponseDto.class
        );

        // Verify the JWT token works
        mockMvc.perform(get("/auth/verify")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"))
                .andExpect(jsonPath("$.user.username").value("testuser@test.com"))
                .andExpect(jsonPath("$.user.role").value("user"));

        // Store user token for other tests
        userJwtToken = loginResponse.getAccessToken();
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void testInvalidCredentials() throws Exception {
        LoginDto invalidLoginDto = new LoginDto();
        invalidLoginDto.setUsername("admin@test.com");
        invalidLoginDto.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidLoginDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with duplicate username")
    void testDuplicateUsernameRegistration() throws Exception {
        RegisterDto duplicateDto = new RegisterDto();
        duplicateDto.setUsername("admin@test.com"); // Already exists
        duplicateDto.setEmail("newemail@test.com");
        duplicateDto.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(duplicateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testDuplicateEmailRegistration() throws Exception {
        RegisterDto duplicateDto = new RegisterDto();
        duplicateDto.setUsername("newusername");
        duplicateDto.setEmail("admin@test.com"); // Already exists
        duplicateDto.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(duplicateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject access with invalid JWT token")
    void testInvalidJwtToken() throws Exception {
        mockMvc.perform(get("/auth/verify")
                .header("Authorization", "Bearer invalid-jwt-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject access without JWT token")
    void testMissingJwtToken() throws Exception {
        mockMvc.perform(get("/auth/verify"))
                .andExpect(status().isUnauthorized());
    }
} 