package com.nutri.hospitalar.localidade.controller;

import com.nutri.hospitalar.catalogo.dtos.CatalogoSelectDto;
import com.nutri.hospitalar.localidade.dtos.CidadeSelectDto;
import com.nutri.hospitalar.localidade.service.LocalidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Estados e municípios do IBGE — referência global, somente leitura, sem
 * filtro por tenant.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Localidades", description = "Estados e municípios do IBGE")
@PreAuthorize("isAuthenticated()")
public class LocalidadeController {

    private final LocalidadeService localidadeService;

    @GetMapping("/estados/select")
    @Operation(summary = "As 27 unidades federativas")
    public ResponseEntity<List<CatalogoSelectDto>> estados() {
        return ResponseEntity.ok(localidadeService.estados());
    }

    @GetMapping("/cidades/select")
    @Operation(summary = "Municípios, para combo",
            description = """
                    Busca no servidor, no máximo 100 por chamada — são mais de
                    cinco mil. `estadoId` restringe à UF, que é como se acha
                    município de nome repetido.""")
    public ResponseEntity<List<CidadeSelectDto>> cidades(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) UUID estadoId) {
        return ResponseEntity.ok(localidadeService.cidades(nome, estadoId));
    }
}
