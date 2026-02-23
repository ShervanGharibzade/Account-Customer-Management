package com.example.ACM.transfer;

import com.example.ACM.enums.LedgerTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferRes(
        String reference,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        LedgerTransactionStatus status,
        LocalDateTime createdAt
) {}