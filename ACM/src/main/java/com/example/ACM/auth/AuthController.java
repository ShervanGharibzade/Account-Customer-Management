package com.example.ACM.auth;

import com.example.ACM.auth.dto.AuthRes;
import com.example.ACM.auth.dto.LoginReq;
import com.example.ACM.auth.dto.RegisterReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthRes> register(@Valid @RequestBody RegisterReq req) {
        AuthRes res = authService.createCustomer(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRes> login(@Valid @RequestBody LoginReq req) {
        AuthRes res = authService.loginCustomer(req);
        return ResponseEntity.ok(res);
    }
}