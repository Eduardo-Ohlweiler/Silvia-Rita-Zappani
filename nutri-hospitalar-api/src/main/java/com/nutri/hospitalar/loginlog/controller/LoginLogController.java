package com.nutri.hospitalar.loginlog.controller;

import com.nutri.hospitalar.loginlog.dtos.LoginLogResponseDto;
import com.nutri.hospitalar.loginlog.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/login-logs")
@RequiredArgsConstructor
@Tag(name = "Log de acesso", description = "Auditoria de autenticação — somente SUPERADMIN")
@PreAuthorize("hasRole('SUPERADMIN')")
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    @Operation(summary = "Log de acesso do tenant efetivo")
    public ResponseEntity<Page<LoginLogResponseDto>> getAll(
            @PageableDefault(size = 20, sort = "dataLogin", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) Boolean sucesso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) String ip) {
        return ResponseEntity.ok(loginLogService.getAll(pageable, usuarioId, sucesso, de, ate, ip));
    }

    @GetMapping("/global")
    @Operation(summary = "Auditoria consolidada de todos os tenants")
    public ResponseEntity<Page<LoginLogResponseDto>> getAllGlobal(
            @PageableDefault(size = 20, sort = "dataLogin", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) Boolean sucesso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) String ip) {
        return ResponseEntity.ok(
                loginLogService.getAllGlobal(pageable, tenantId, usuarioId, sucesso, de, ate, ip));
    }
}
