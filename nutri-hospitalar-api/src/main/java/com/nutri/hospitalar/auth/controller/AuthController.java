package com.nutri.hospitalar.auth.controller;

import com.nutri.hospitalar.auth.dtos.AuthResponseDto;
import com.nutri.hospitalar.auth.dtos.LoginDto;
import com.nutri.hospitalar.auth.dtos.RefreshDto;
import com.nutri.hospitalar.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autentica e devolve o par de tokens")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto dto,
                                                 HttpServletRequest request) {
        return ResponseEntity.ok(authService.login(dto, request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotaciona o par de tokens",
            description = "Uso único: o token apresentado é revogado e um novo par é emitido.")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshDto dto) {
        return ResponseEntity.ok(authService.refresh(dto.refreshToken()));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Encerra a sessão e revoga os refresh tokens")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/switch-tenant/{tenantId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Entra em um tenant",
            description = """
                    Reemite o access token com o tenant de destino e
                    `impersonating: true`. O superadmin passa a enxergar
                    exatamente o que um usuário daquele tenant enxerga —
                    o filtro de tenant continua valendo, só muda o valor.""")
    public ResponseEntity<AuthResponseDto> switchTenant(@PathVariable UUID tenantId,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(authService.switchTenant(tenantId, request));
    }

    @PostMapping("/exit-tenant")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Volta ao tenant de origem")
    public ResponseEntity<AuthResponseDto> exitTenant(HttpServletRequest request) {
        return ResponseEntity.ok(authService.exitTenant(request));
    }
}
