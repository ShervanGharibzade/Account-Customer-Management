package com.example.ACM.transfer;

import com.example.ACM.account.AccountService;
import com.example.ACM.customer.Customer;
import com.example.ACM.ledger.LedgerTransaction;
import com.example.ACM.repository.LedgerTransactionRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final LedgerTransactionRepo ledgerTransactionRepo;
    private final AccountService accountService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferRes> transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferReq req,
            @AuthenticationPrincipal Customer customer) {

        // اگر این کلید قبلاً پردازش شده، نتیجه قبلی را برگردان
        if (idempotencyKey != null) {
            Optional<LedgerTransaction> existing = ledgerTransactionRepo.findByReference(idempotencyKey);
            if (existing.isPresent()) {
                return ResponseEntity.ok(mapToTransferRes(existing.get(), req));
            }
        }

        TransferRes res = transferService.transfer(req, customer.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // helper — LedgerTransaction را به TransferRes تبدیل می‌کند
    private TransferRes mapToTransferRes(LedgerTransaction tx, TransferReq req) {
        return new TransferRes(
                tx.getReference(),
                req.fromAccountNumber(),
                req.toAccountNumber(),
                req.amount(),
                tx.getStatus(),
                tx.getCreatedAt()
        );
    }
}