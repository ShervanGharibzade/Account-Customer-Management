package com.example.ACM.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepo extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransaction(LedgerTransaction transaction);
}