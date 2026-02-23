package com.example.ACM.account;

import com.example.ACM.account.dto.AccountRes;
import com.example.ACM.account.dto.OpenAccountReq;
import com.example.ACM.customer.Customer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountRes> openAccount(
            @Valid @RequestBody OpenAccountReq req,
            @AuthenticationPrincipal Customer customer) {

        AccountRes res = accountService.openAccount(customer.getId(), req.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AccountRes> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AccountRes>> getMyAccounts(
            @AuthenticationPrincipal Customer customer) {
        return ResponseEntity.ok(accountService.getCustomerAccounts(customer.getId()));
    }

    @DeleteMapping("/{accountNumber}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> closeAccount(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal Customer customer) {
        accountService.closeAccount(accountNumber, customer.getId());
        return ResponseEntity.noContent().build();
    }
}