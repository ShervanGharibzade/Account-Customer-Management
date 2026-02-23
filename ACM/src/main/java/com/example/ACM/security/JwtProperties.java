package com.example.ACM.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "security.jwt")
@Component
@Getter
public class JwtProperties {
    private String secret;
    private long expiration;
}