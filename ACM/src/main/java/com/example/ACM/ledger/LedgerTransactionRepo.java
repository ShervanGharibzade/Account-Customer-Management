package com.example.ACM.ledger;

import com.example.ACM.enums.LedgerTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerTransactionRepo extends JpaRepository<LedgerTransaction, Long> {
    Optional<LedgerTransaction> findByReference(String reference);
    Page<LedgerTransaction> findByStatus(LedgerTransactionStatus status, Pageable pageable);
}