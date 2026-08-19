package com.nutri.hospitalar.pediatria.controller;

import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaCreateDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaResponseDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaSelectDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaUpdateDto;
import com.nutri.hospitalar.pediatria.service.FormulaLacteaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/formulas-lacteas")
@RequiredArgsConstructor
@Tag(name = "Fórmulas lácteas")
@PreAuthorize("isAuthenticated()")
public class FormulaLacteaController {

    private final FormulaLacteaService formulaLacteaService;

    @GetMapping
    @Operation(summary = "Lista as fórmulas lácteas",
            description = """
                    Traz as fórmulas do cliente e as globais do sistema. O filtro
                    `global` separa umas das outras: `true` só as do sistema,
                    `false` só as próprias, ausente traz as duas.
                    """)
    public ResponseEntity<Page<FormulaLacteaResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean global) {
        return ResponseEntity.ok(formulaLacteaService.getAll(pageable, nome, ativo, global));
    }

    @GetMapping("/select")
    @Operation(summary = "Fórmulas ativas para combo",
            description = """
                    Leva a composição junto: a tela de cálculo mostra
                    "NAN 2 (73,8 kcal · 1,65 g / 100 ml)" no rótulo da opção.
                    """)
    public ResponseEntity<List<FormulaLacteaSelectDto>> select(
            @RequestParam(required = false) String termo) {
        return ResponseEntity.ok(formulaLacteaService.select(termo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma fórmula láctea")
    public ResponseEntity<FormulaLacteaResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(formulaLacteaService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma fórmula do cliente")
    public ResponseEntity<FormulaLacteaResponseDto> create(
            @Valid @RequestBody FormulaLacteaCreateDto dto) {
        FormulaLacteaResponseDto criada = formulaLacteaService.create(dto);
        return ResponseEntity.created(URI.create("/formulas-lacteas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera uma fórmula do cliente",
            description = """
                    Fórmula global do sistema devolve 400: ela é visível para
                    todos e editável por ninguém. Cadastre uma própria para ter
                    outra composição.
                    """)
    public ResponseEntity<FormulaLacteaResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody FormulaLacteaUpdateDto dto) {
        return ResponseEntity.ok(formulaLacteaService.update(id, dto));
    }

    @PatchMapping("/{id}/ativo")
    @Operation(summary = "Ativa ou desativa uma fórmula do cliente",
            description = """
                    Desativar tira a fórmula do combo sem apagar histórico: as
                    avaliações guardam o retrato da composição usada.
                    """)
    public ResponseEntity<FormulaLacteaResponseDto> alterarAtivo(
            @PathVariable UUID id, @RequestParam boolean ativo) {
        return ResponseEntity.ok(formulaLacteaService.alterarAtivo(id, ativo));
    }
}
