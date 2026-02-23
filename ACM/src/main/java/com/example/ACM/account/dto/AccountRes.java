package com.example.ACM.account.dto;

import com.example.ACM.enums.AccountStatus;
import com.example.ACM.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountRes(
        Long id,
        String accountNumber,
        AccountType type,
        AccountStatus status,
        BigDecimal balance,
        LocalDateTime createdAt
) {}