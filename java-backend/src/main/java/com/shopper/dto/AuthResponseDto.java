package com.shopper.dto;

import com.shopper.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    
    private String accessToken;
    private String tokenType = "Bearer";
    private UserInfo user;
    
    public AuthResponseDto(String accessToken, User user) {
        this.accessToken = accessToken;
        this.user = new UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole().getValue());
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String username;
        private String email;
        private String role;
    }
}