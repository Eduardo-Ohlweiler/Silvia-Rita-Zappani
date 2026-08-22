package com.nutri.hospitalar.uti.controller;

import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalCreateDto;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalResponseDto;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalSelectDto;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalUpdateDto;
import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import com.nutri.hospitalar.uti.enums.TipoProdutoNutricional;
import com.nutri.hospitalar.uti.service.ProdutoNutricionalService;
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

/**
 * As rotas literais — {@code /select}, {@code /modulos-proteicos},
 * {@code /insumos-artesanais} — vêm antes de {@code /{id}} porque o Spring
 * prefere o literal. Ver a armadilha em {@code CLAUDE.md}.
 */
@RestController
@RequestMapping("/produtos-nutricionais")
@RequiredArgsConstructor
@Tag(name = "Produtos nutricionais")
@PreAuthorize("isAuthenticated()")
public class ProdutoNutricionalController {

    private final ProdutoNutricionalService produtoNutricionalService;

    @GetMapping
    @Operation(summary = "Lista os produtos nutricionais",
            description = """
                    Suplementos orais, módulos proteicos e insumos de dieta
                    artesanal no mesmo catálogo — o filtro `tipo` separa. Traz os
                    do cliente e os globais do sistema; `global` separa uns dos
                    outros.
                    """)
    public ResponseEntity<Page<ProdutoNutricionalResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) TipoProdutoNutricional tipo,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean global) {
        return ResponseEntity.ok(
                produtoNutricionalService.getAll(pageable, nome, tipo, ativo, global));
    }

    @GetMapping("/select")
    @Operation(summary = "Produtos ativos para combo",
            description = """
                    Leva a medida e a composição junto: o rótulo da opção mostra
                    "Trophic Basic (medida 7,8 g · 30 kcal · 1,2 g PTN)". Filtre
                    por `tipo` para não misturar suplemento com insumo.
                    """)
    public ResponseEntity<List<ProdutoNutricionalSelectDto>> select(
            @RequestParam(required = false) TipoProdutoNutricional tipo,
            @RequestParam(required = false) String termo) {
        return ResponseEntity.ok(produtoNutricionalService.select(tipo, termo));
    }

    @GetMapping("/modulos-proteicos")
    @Operation(summary = "Módulos proteicos que a dieta enteral oferece",
            description = """
                    É o catálogo que alimenta a sugestão de módulo — não uma lista
                    fixa no código. Cadastrar um módulo novo faz efeito aqui.
                    """)
    public ResponseEntity<List<ProdutoNutricionalSelectDto>> modulosProteicos() {
        return ResponseEntity.ok(produtoNutricionalService.modulosProteicos());
    }

    @GetMapping("/insumos-artesanais")
    @Operation(summary = "Insumos de um papel da receita artesanal",
            description = """
                    Os quatro papéis são fixos (base, carboidrato, proteína,
                    lipídio) e o catálogo diz qual produto ocupa cada um. O
                    insumo do próprio cliente vem antes do global na lista.
                    """)
    public ResponseEntity<List<ProdutoNutricionalSelectDto>> insumosArtesanais(
            @RequestParam PapelArtesanal papel) {
        return ResponseEntity.ok(produtoNutricionalService.insumosPorPapel(papel));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um produto nutricional")
    public ResponseEntity<ProdutoNutricionalResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(produtoNutricionalService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um produto do cliente",
            description = """
                    A composição vai **por medida**, e a medida se declara em
                    `medidaNome` e `medidaQtd`.

                    Duas regras dependem do tipo: insumo artesanal exige o papel
                    na receita (e os outros tipos o proibem), e módulo proteico e
                    insumo exigem calorias e proteína — eles entram em cálculo, e
                    sem composição a dose sairia nula sem avisar.

                    `moduloProteico` não é aceito no corpo: o servidor o deriva
                    do tipo.
                    """)
    public ResponseEntity<ProdutoNutricionalResponseDto> create(
            @Valid @RequestBody ProdutoNutricionalCreateDto dto) {
        ProdutoNutricionalResponseDto criado = produtoNutricionalService.create(dto);
        return ResponseEntity.created(
                URI.create("/produtos-nutricionais/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um produto do cliente",
            description = """
                    Produto global do sistema devolve 400: visível para todos,
                    editável por ninguém. Cadastre um próprio para ter outra
                    composição.
                    """)
    public ResponseEntity<ProdutoNutricionalResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody ProdutoNutricionalUpdateDto dto) {
        return ResponseEntity.ok(produtoNutricionalService.update(id, dto));
    }

    @PatchMapping("/{id}/ativo")
    @Operation(summary = "Ativa ou desativa um produto do cliente",
            description = """
                    Desativar tira o produto dos combos sem apagar histórico: as
                    avaliações guardam o retrato da composição usada.
                    """)
    public ResponseEntity<ProdutoNutricionalResponseDto> alterarAtivo(
            @PathVariable UUID id, @RequestParam boolean ativo) {
        return ResponseEntity.ok(produtoNutricionalService.alterarAtivo(id, ativo));
    }
}
