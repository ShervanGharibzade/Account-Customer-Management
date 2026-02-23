package com.example.ACM.auth;

import com.example.ACM.auth.dto.AuthRes;
import com.example.ACM.auth.dto.LoginReq;
import com.example.ACM.auth.dto.RegisterReq;
import com.example.ACM.customer.Customer;
import com.example.ACM.enums.CustomerRole;
import com.example.ACM.enums.CustomerStatus;
import com.example.ACM.exception.ResourceAlreadyExistsException;
import com.example.ACM.exception.ResourceNotFoundException;
import com.example.ACM.repository.CustomerRepo;
import com.example.ACM.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepo customerRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthRes createCustomer(RegisterReq req) {
        if (customerRepo.findByUsername(req.username()).isPresent()) {
            throw new ResourceAlreadyExistsException("User", "username", req.username());
        }

        if (customerRepo.findByNationalId(req.nationalId()).isPresent()) {
            throw new ResourceAlreadyExistsException("User", "national id", req.nationalId());
        }

        Customer customer = Customer.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .nationalId(req.nationalId())
                .role(CustomerRole.USER)
                .status(CustomerStatus.ACTIVE)
                .build();

        customerRepo.save(customer);
        log.info("New customer registered: {}", customer.getUsername());

        String token = jwtUtil.generateToken(customer);
        return new AuthRes(token, customer.getUsername());
    }

    @Transactional()
    public AuthRes loginCustomer(LoginReq req) {
        Customer customer = customerRepo.findByUsername(req.username())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", req.username()));

        if (!passwordEncoder.matches(req.password(), customer.getPassword())) {
            log.warn("Failed login attempt for username: {}", req.username());
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(customer);
        log.info("Customer logged in: {}", customer.getUsername());

        return new AuthRes(token, customer.getUsername());
    }
}