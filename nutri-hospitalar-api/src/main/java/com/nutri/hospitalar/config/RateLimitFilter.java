package com.nutri.hospitalar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutri.hospitalar.exceptions.ErroResponseDto;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> ROTAS_PROTEGIDAS = Set.of(
            "/auth/login",
            "/auth/refresh");

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Bucket> baldes = new ConcurrentHashMap<>();

    @Value("${app.seguranca.rate-limit-por-minuto:5}")
    private int limitePorMinuto;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !ROTAS_PROTEGIDAS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        Bucket balde = baldes.computeIfAbsent(chaveDe(request), k -> novoBalde());

        if (balde.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErroResponseDto.de(
                "Muitas tentativas. Aguarde um minuto e tente novamente.",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                request.getRequestURI()));
    }

    private Bucket novoBalde() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limitePorMinuto)
                        .refillIntervally(limitePorMinuto, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String chaveDe(HttpServletRequest request) {
        return ipDe(request) + "|" + request.getRequestURI();
    }

    static String ipDe(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank())
            return encaminhado.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
