package com.example.ACM.payment;

import com.example.ACM.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_reference_id",columnList = "reference_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true,name = "reference_id")
    private String referenceId;

    @NotNull
    @Positive
    @DecimalMin(value = "0.01",inclusive = true)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false,name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "from_account")
    private String fromAccountNumber;

    @Column(name = "to_account")
    private String toAccountNumber;

    @Column(name = "ledger_reference")
    private String ledgerReference;

    @Column(name = "failure_reason")
    private String failureReason;
}
