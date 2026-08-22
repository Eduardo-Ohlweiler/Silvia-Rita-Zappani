package com.nutri.hospitalar.uti.controller;

import com.nutri.hospitalar.uti.dtos.FormulaEnteralCreateDto;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralResponseDto;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralSelectDto;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralUpdateDto;
import com.nutri.hospitalar.uti.enums.CategoriaFormulaEnteral;
import com.nutri.hospitalar.uti.service.FormulaEnteralService;
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
@RequestMapping("/formulas-enterais")
@RequiredArgsConstructor
@Tag(name = "Fórmulas enterais")
@PreAuthorize("isAuthenticated()")
public class FormulaEnteralController {

    private final FormulaEnteralService formulaEnteralService;

    @GetMapping
    @Operation(summary = "Lista as fórmulas enterais",
            description = """
                    Traz as fórmulas do cliente e as globais do sistema. O filtro
                    `global` separa umas das outras: `true` só as do sistema,
                    `false` só as próprias, ausente traz as duas.

                    **Toda composição é por litro** — inclusive a de produto que
                    vem em frasco de 500 ml.
                    """)
    public ResponseEntity<Page<FormulaEnteralResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) CategoriaFormulaEnteral categoria,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean global) {
        return ResponseEntity.ok(
                formulaEnteralService.getAll(pageable, nome, categoria, ativo, global));
    }

    @GetMapping("/select")
    @Operation(summary = "Fórmulas ativas para combo",
            description = """
                    Leva a composição junto: a tela de dieta mostra
                    "Peptamen Intense (1,0 kcal/ml · 92 g PTN/L)" no rótulo da
                    opção — quem prescreve escolhe pela composição.

                    `aguaLivrePerc` vem anulável de propósito: é a ausência dela
                    que faz a aba de hidratação dizer que estimou a água pela
                    densidade em vez de ler o rótulo.
                    """)
    public ResponseEntity<List<FormulaEnteralSelectDto>> select(
            @RequestParam(required = false) String termo) {
        return ResponseEntity.ok(formulaEnteralService.select(termo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma fórmula enteral")
    public ResponseEntity<FormulaEnteralResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(formulaEnteralService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma fórmula do cliente",
            description = """
                    A composição vai **por litro**. Se os macros não fecharem com
                    a densidade declarada (4·PTN + 4·CHO + 9·LIP, tolerância de
                    12 %), o cadastro é recusado com o desvio e a causa provável
                    — foi essa conta que encontrou quatro produtos errados na
                    planilha de origem.
                    """)
    public ResponseEntity<FormulaEnteralResponseDto> create(
            @Valid @RequestBody FormulaEnteralCreateDto dto) {
        FormulaEnteralResponseDto criada = formulaEnteralService.create(dto);
        return ResponseEntity.created(URI.create("/formulas-enterais/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera uma fórmula do cliente",
            description = """
                    Fórmula global do sistema devolve 400: ela é visível para
                    todos e editável por ninguém. Cadastre uma própria para ter
                    outra composição.
                    """)
    public ResponseEntity<FormulaEnteralResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody FormulaEnteralUpdateDto dto) {
        return ResponseEntity.ok(formulaEnteralService.update(id, dto));
    }

    @PatchMapping("/{id}/ativo")
    @Operation(summary = "Ativa ou desativa uma fórmula do cliente",
            description = """
                    Desativar tira a fórmula do combo sem apagar histórico: as
                    avaliações guardam o retrato da composição usada.
                    """)
    public ResponseEntity<FormulaEnteralResponseDto> alterarAtivo(
            @PathVariable UUID id, @RequestParam boolean ativo) {
        return ResponseEntity.ok(formulaEnteralService.alterarAtivo(id, ativo));
    }
}
