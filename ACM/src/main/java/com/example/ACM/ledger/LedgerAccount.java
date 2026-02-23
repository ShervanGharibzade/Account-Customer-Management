package com.example.ACM.ledger;

import com.example.ACM.enums.LedgerAccountType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ledger_accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. CASH, CUSTOMER_123

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccountType type;

    @Column(nullable = false)
    private String name;
}
