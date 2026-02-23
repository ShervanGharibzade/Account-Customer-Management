package com.example.ACM.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUsername(String username);
    Optional<Customer> findByNationalId(String nationalId);
    boolean existsByUsername(String username);
    boolean existsByNationalId(String nationalId);
}