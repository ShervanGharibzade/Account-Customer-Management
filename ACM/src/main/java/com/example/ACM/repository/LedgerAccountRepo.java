package com.example.ACM.repository;


import com.example.ACM.ledger.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerAccountRepo extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByCode(String code);
}