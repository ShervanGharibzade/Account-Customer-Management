package com.example.ACM.account;

import com.example.ACM.account.dto.AccountRes;
import com.example.ACM.auditLog.AuditLogService;
import com.example.ACM.customer.Customer;
import com.example.ACM.enums.AccountStatus;
import com.example.ACM.enums.AccountType;
import com.example.ACM.enums.LedgerAccountType;
import com.example.ACM.exception.ResourceNotFoundException;
import com.example.ACM.ledger.LedgerAccount;
import com.example.ACM.customer.CustomerRepo;
import com.example.ACM.ledger.LedgerAccountRepo;
import com.example.ACM.transfer.TransferReq;
import com.example.ACM.transfer.TransferRes;
import com.example.ACM.transfer.TransferService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepo accountRepo;
    private final CustomerRepo customerRepo;
    private final LedgerAccountRepo ledgerAccountRepo;
    private final AuditLogService auditLogService;
    private final TransferService transferService;

    @Transactional
    public AccountRes openAccount(Long customerId, AccountType type) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .type(type)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .customer(customer)
                .build();

        Account saved = accountRepo.save(account);

        LedgerAccount ledgerAccount = LedgerAccount.builder()
                .code("CUSTOMER_" + customerId + "_" + accountNumber)
                .name(customer.getFullName() + " - " + type.name())
                .type(LedgerAccountType.LIABILITY)
                .build();
        ledgerAccountRepo.save(ledgerAccount);

        auditLogService.log("ACCOUNT_OPENED", "Account", accountNumber, customerId, "Type: " + type);
        log.info("Account opened: {} for customer: {}", accountNumber, customerId);

        return mapToRes(saved);
    }

    public AccountRes getAccount(String accountNumber) {
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
        return mapToRes(account);
    }

    public List<AccountRes> getCustomerAccounts(Long customerId) {
        return accountRepo.findByCustomerId(customerId)
                .stream()
                .map(this::mapToRes)
                .toList();
    }

    @Transactional
    public void closeAccount(String accountNumber, Long performedBy) {
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepo.save(account);
        auditLogService.log("ACCOUNT_CLOSED", "Account", accountNumber, performedBy, "");
    }

    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    public AccountRes mapToRes(Account a) {
        return new AccountRes(a.getId(), a.getAccountNumber(), a.getType(),
                a.getStatus(), a.getBalance(), a.getCreatedAt());
    }

    @Transactional
    public TransferRes transfer(TransferReq req, Long userId) {
        try {
            return transferService.transfer(req, userId);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Transfer failed due to concurrent modification. Please retry.", e);
        }
    }
}