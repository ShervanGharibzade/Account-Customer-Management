package com.example.ACM.admin;

import com.example.ACM.account.Account;
import com.example.ACM.customer.Customer;
import com.example.ACM.enums.AccountStatus;
import com.example.ACM.exception.ResourceNotFoundException;
import com.example.ACM.ledger.LedgerTransaction;
import com.example.ACM.repository.AccountRepo;
import com.example.ACM.repository.CustomerRepo;
import com.example.ACM.repository.LedgerTransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final CustomerRepo customerRepo;
    private final AccountRepo accountRepo;
    private final LedgerTransactionRepo ledgerRepo;

    @GetMapping("/customers")
    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepo.findAll(pageable);
    }

    @PutMapping("/accounts/{accountNumber}/block")
    public ResponseEntity<Void> blockAccount(@PathVariable String accountNumber) {
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "number", accountNumber));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepo.save(account);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ledger/transactions")
    public Page<LedgerTransaction> getLedgerTransactions(Pageable pageable) {
        return ledgerRepo.findAll(pageable);
    }
}