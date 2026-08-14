package com.nutri.hospitalar.catalogo.controller;

import com.nutri.hospitalar.catalogo.dtos.CatalogoSelectDto;
import com.nutri.hospitalar.catalogo.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogos do cadastro de pessoas — somente leitura, para alimentar combo.
 *
 * <p>Não filtram por tenant: são dados de referência, iguais para todo cliente.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Catálogos", description = "Listas de referência do cadastro de pessoas")
@PreAuthorize("isAuthenticated()")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/tipos-cadastro/select")
    @Operation(summary = "Tipos de cadastro (Paciente, Responsável, …)")
    public ResponseEntity<List<CatalogoSelectDto>> tiposCadastro() {
        return ResponseEntity.ok(catalogoService.tiposCadastro());
    }

    @GetMapping("/tipos-telefone/select")
    @Operation(summary = "Tipos de telefone")
    public ResponseEntity<List<CatalogoSelectDto>> tiposTelefone() {
        return ResponseEntity.ok(catalogoService.tiposTelefone());
    }

    @GetMapping("/tipos-email/select")
    @Operation(summary = "Tipos de e-mail")
    public ResponseEntity<List<CatalogoSelectDto>> tiposEmail() {
        return ResponseEntity.ok(catalogoService.tiposEmail());
    }

    @GetMapping("/tipos-rede-social/select")
    @Operation(summary = "Tipos de rede social")
    public ResponseEntity<List<CatalogoSelectDto>> tiposRedeSocial() {
        return ResponseEntity.ok(catalogoService.tiposRedeSocial());
    }

    @GetMapping("/tipos-endereco/select")
    @Operation(summary = "Tipos de endereço")
    public ResponseEntity<List<CatalogoSelectDto>> tiposEndereco() {
        return ResponseEntity.ok(catalogoService.tiposEndereco());
    }
}
