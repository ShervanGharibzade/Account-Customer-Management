package com.example.ACM.transfer;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferReq(
        @NotBlank(message = "Source account number is required")
        String fromAccountNumber,

        @NotBlank(message = "Destination account number is required")
        String toAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        @DecimalMin(value = "0.01", message = "Minimum transfer amount is 0.01")
        @DecimalMax(value = "1000000.00", message = "Maximum transfer amount is 1,000,000")
        BigDecimal amount,

        @NotBlank
        @Size(min = 3, max = 255, message = "Description must be between 3 and 255 characters")
        String description
) {}