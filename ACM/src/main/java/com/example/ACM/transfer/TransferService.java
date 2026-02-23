package com.example.ACM.transfer;

import com.example.ACM.account.Account;
import com.example.ACM.auditLog.AuditLogService;
import com.example.ACM.enums.AccountStatus;
import com.example.ACM.enums.EntryType;
import com.example.ACM.enums.LedgerTransactionStatus;
import com.example.ACM.exception.ResourceNotFoundException;
import com.example.ACM.ledger.LedgerAccount;
import com.example.ACM.ledger.LedgerEntry;
import com.example.ACM.ledger.LedgerTransaction;
import com.example.ACM.repository.AccountRepo;
import com.example.ACM.repository.LedgerAccountRepo;
import com.example.ACM.repository.LedgerEntryRepo;
import com.example.ACM.repository.LedgerTransactionRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepo accountRepo;
    private final LedgerAccountRepo ledgerAccountRepo;
    private final LedgerTransactionRepo ledgerTransactionRepo;
    private final LedgerEntryRepo ledgerEntryRepo;
    private final AuditLogService auditLogService;

    // متد public — Optimistic Lock را هندل می‌کند و doTransfer را صدا می‌زند
    @Transactional
    public TransferRes transfer(TransferReq req, Long userId) {
        try {
            return doTransfer(req, userId);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException(
                    "Transfer failed due to concurrent modification. Please retry.", e);
        }
    }

    // متد برای idempotency — اگر قبلاً پردازش شده، همان نتیجه را برمی‌گرداند
    public Optional<TransferRes> findByIdempotencyKey(String key, TransferReq req) {
        return ledgerTransactionRepo.findByReference(key)
                .map(tx -> new TransferRes(
                        tx.getReference(),
                        req.fromAccountNumber(),
                        req.toAccountNumber(),
                        req.amount(),
                        tx.getStatus(),
                        tx.getCreatedAt()
                ));
    }

    // متد private — تمام منطق اصلی انتقال وجه اینجاست
    private TransferRes doTransfer(TransferReq req, Long performedBy) {

        // ۱. قفل کردن حساب‌ها با PESSIMISTIC_WRITE (جلوگیری از Deadlock با ترتیب ثابت)
        Account from = accountRepo.findByAccountNumberForUpdate(req.fromAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "number", req.fromAccountNumber()));
        Account to = accountRepo.findByAccountNumberForUpdate(req.toAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "number", req.toAccountNumber()));

        // ۲. اعتبارسنجی
        validateTransfer(from, to, req.amount());

        // ۳. ایجاد LedgerTransaction با وضعیت PENDING
        String reference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LedgerTransaction transaction = LedgerTransaction.builder()
                .reference(reference)
                .description(req.description())
                .status(LedgerTransactionStatus.PENDING)
                .build();
        ledgerTransactionRepo.save(transaction);

        // ۴. پیدا کردن Ledger Accounts متناظر
        LedgerAccount fromLedger = ledgerAccountRepo
                .findByCode("CUSTOMER_" + from.getCustomer().getId() + "_" + from.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("LedgerAccount", "from", from.getAccountNumber()));

        LedgerAccount toLedger = ledgerAccountRepo
                .findByCode("CUSTOMER_" + to.getCustomer().getId() + "_" + to.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("LedgerAccount", "to", to.getAccountNumber()));

        // ۵. ایجاد Ledger Entries (دو طرفه — DEBIT و CREDIT)
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transaction(transaction)
                .ledgerAccount(fromLedger)
                .entryType(EntryType.DEBIT)
                .amount(req.amount())
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transaction(transaction)
                .ledgerAccount(toLedger)
                .entryType(EntryType.CREDIT)
                .amount(req.amount())
                .build();

        ledgerEntryRepo.save(debitEntry);
        ledgerEntryRepo.save(creditEntry);

        // ۶. به‌روزرسانی موجودی حساب‌ها
        from.setBalance(from.getBalance().subtract(req.amount()));
        to.setBalance(to.getBalance().add(req.amount()));
        accountRepo.save(from);
        accountRepo.save(to);

        // ۷. تغییر وضعیت تراکنش به COMPLETED
        transaction.setStatus(LedgerTransactionStatus.COMPLETED);
        ledgerTransactionRepo.save(transaction);

        // ۸. ثبت Audit Log
        auditLogService.log("TRANSFER", "LedgerTransaction", reference, performedBy,
                "From: " + req.fromAccountNumber() +
                        " To: " + req.toAccountNumber() +
                        " Amount: " + req.amount());

        log.info("Transfer completed: {} | {} -> {} | Amount: {}",
                reference, req.fromAccountNumber(), req.toAccountNumber(), req.amount());

        return new TransferRes(
                reference,
                req.fromAccountNumber(),
                req.toAccountNumber(),
                req.amount(),
                LedgerTransactionStatus.COMPLETED,
                transaction.getCreatedAt()
        );
    }

    private void validateTransfer(Account from, Account to, BigDecimal amount) {
        if (from.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Source account is not active");
        }
        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Destination account is not active");
        }
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance. Available: " + from.getBalance());
        }
    }
}