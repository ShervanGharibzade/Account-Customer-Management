package com.example.ACM.repository;

import com.example.ACM.enums.PaymentStatus;
import com.example.ACM.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReferenceId(String referenceId);

    List<Payment> findByStatus(PaymentStatus paymentStatus);
}
