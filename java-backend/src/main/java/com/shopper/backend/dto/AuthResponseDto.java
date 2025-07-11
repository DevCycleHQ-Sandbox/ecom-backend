package com.shopper.backend.dto;

import com.shopper.backend.entity.User;

public class AuthResponseDto {
    
    private UserDto user;
    private String token;
    
    // Constructors
    public AuthResponseDto() {}
    
    public AuthResponseDto(UserDto user, String token) {
        this.user = user;
        this.token = token;
    }
    
    // Getters and Setters
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}