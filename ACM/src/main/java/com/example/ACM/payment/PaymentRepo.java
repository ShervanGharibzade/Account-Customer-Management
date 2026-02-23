package com.example.ACM.payment;

import com.example.ACM.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReferenceId(String referenceId);

    List<Payment> findByStatus(PaymentStatus paymentStatus);
}
