package com.example.ACM.account.dto;

import com.example.ACM.enums.AccountType;
import jakarta.validation.constraints.NotNull;

public record OpenAccountReq(
        @NotNull AccountType type
) {}