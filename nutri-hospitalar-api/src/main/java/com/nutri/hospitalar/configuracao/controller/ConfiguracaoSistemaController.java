package com.nutri.hospitalar.configuracao.controller;

import com.nutri.hospitalar.configuracao.dtos.AparenciaResponseDto;
import com.nutri.hospitalar.configuracao.dtos.AparenciaUpdateDto;
import com.nutri.hospitalar.configuracao.service.ConfiguracaoSistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/configuracoes")
@RequiredArgsConstructor
@Tag(name = "Configurações gerais", description = "Configuração do sistema — leitura pública, escrita do SUPERADMIN")
public class ConfiguracaoSistemaController {

    private final ConfiguracaoSistemaService service;

    /**
     * Pública, e é a única rota pública que não é de autenticação: a tela de
     * login precisa abrir no tema antes de existir sessão. Devolve só o nome
     * da paleta — ver {@link AparenciaResponseDto}.
     */
    @GetMapping("/aparencia")
    @SecurityRequirements
    @Operation(summary = "Paleta de cores em uso no sistema")
    public ResponseEntity<AparenciaResponseDto> aparencia() {
        return ResponseEntity.ok(service.aparencia());
    }

    @PutMapping("/aparencia")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Define a paleta de cores para todos os usuários")
    public ResponseEntity<AparenciaResponseDto> atualizarAparencia(@Valid @RequestBody AparenciaUpdateDto dto) {
        return ResponseEntity.ok(service.atualizarAparencia(dto));
    }
}
