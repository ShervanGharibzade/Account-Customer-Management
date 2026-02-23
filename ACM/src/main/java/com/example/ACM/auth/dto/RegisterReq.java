package com.example.ACM.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterReq(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 50, message = "Username must be 4-50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "National ID is required")
        @Pattern(regexp = "\\d{10}", message = "National ID must be 10 digits")
        String nationalId
) {}