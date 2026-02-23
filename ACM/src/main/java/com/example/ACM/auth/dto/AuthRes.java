package com.example.ACM.auth.dto;


public record AuthRes(
        String accessToken,
        String username,
        String tokenType
) {
    public AuthRes(String accessToken, String username) {
        this(accessToken, username, "Bearer");
    }
}