package com.nutri.hospitalar.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private Duration accessTtl = Duration.ofHours(8);
    private Duration refreshTtl = Duration.ofDays(7);

    @PostConstruct
    void validar() {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException(
                    "JWT_SECRET precisa de no mínimo 32 caracteres para HMAC-SHA256");

        if (accessTtl.compareTo(Duration.ofHours(8)) > 0)
            throw new IllegalStateException(
                    "jwt.access-ttl não pode passar de 8 horas (OWASP A02)");
    }
}
