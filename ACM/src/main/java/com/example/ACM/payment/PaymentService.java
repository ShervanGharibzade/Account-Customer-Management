package com.example.ACM.payment;

import com.example.ACM.auditLog.AuditLogService;
import com.example.ACM.enums.PaymentStatus;
import com.example.ACM.exception.ResourceNotFoundException;
import com.example.ACM.transfer.TransferReq;
import com.example.ACM.transfer.TransferRes;
import com.example.ACM.transfer.TransferService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepo repo;
    private final TransferService transferService;
    private final AuditLogService audit;

    @Transactional
    public Payment initiatePayment(BigDecimal amount, String fromAccount,
                                   String toAccount, Long userId) {
        Payment payment = Payment.builder()
                .referenceId(UUID.randomUUID().toString())
                .amount(amount)
                .fromAccountNumber(fromAccount)
                .toAccountNumber(toAccount)
                .status(PaymentStatus.INITIATED)
                .build();

        Payment saved = repo.save(payment);
        audit.log("PAYMENT_INITIATED", "Payment", saved.getReferenceId(), userId,
                "Amount: " + amount);
        return saved;
    }

    @Transactional
    public Payment processPayment(String referenceId, Long userId) {
        Payment payment = repo.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "ref", referenceId));

        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Payment already processed: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        repo.save(payment);

        try {
            TransferReq req = new TransferReq(
                    payment.getFromAccountNumber(),
                    payment.getToAccountNumber(),
                    payment.getAmount(),
                    "Payment: " + referenceId
            );
            TransferRes result = transferService.transfer(req, userId);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setLedgerReference(result.reference());
            audit.log("PAYMENT_COMPLETED", "Payment", referenceId, userId, "");

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            audit.log("PAYMENT_FAILED", "Payment", referenceId, userId, e.getMessage());
            log.error("Payment failed: {}", referenceId, e);
        }

        return repo.save(payment);
    }

    // پردازش خودکار با @Scheduled
    @Scheduled(fixedDelay = 30000) // هر ۳۰ ثانیه
    @Transactional
    public void processInitiatedPayments() {
        List<Payment> initiated = repo.findByStatus(PaymentStatus.INITIATED);
        log.info("Processing {} initiated payments", initiated.size());
        initiated.forEach(p -> processPayment(p.getReferenceId(), null));
    }
}