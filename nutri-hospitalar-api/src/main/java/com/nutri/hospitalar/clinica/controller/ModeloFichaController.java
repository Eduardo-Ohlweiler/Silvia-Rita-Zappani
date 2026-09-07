package com.nutri.hospitalar.clinica.controller;

import com.nutri.hospitalar.clinica.dtos.ModeloFichaCreateDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaResponseDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaSelectDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaUpdateDto;
import com.nutri.hospitalar.clinica.service.ModeloFichaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Modelos de ficha de anamnese.
 *
 * <p>Módulo de negócio: {@code isAuthenticated()}, e não role — {@code ADMIN} e
 * {@code USER} operam. O isolamento vem do tenant efetivo do token.
 *
 * <p><b>Rota literal antes de {@code /{id}}</b>: {@code /select} convive com
 * {@code /{id}} porque o Spring prefere o literal.
 */
@RestController
@RequestMapping("/modelos-ficha")
@RequiredArgsConstructor
@Tag(name = "Modelos de ficha de anamnese")
@PreAuthorize("isAuthenticated()")
public class ModeloFichaController {

    private final ModeloFichaService modeloFichaService;

    @GetMapping
    @Operation(summary = "Lista os modelos de ficha",
            description = """
                    Traz os modelos do cliente e os do sistema. O filtro
                    `doSistema` separa uns dos outros: `true` só os do sistema,
                    `false` só os próprios, ausente traz os dois.

                    **Modelo do sistema é imutável** — visível a todo cliente,
                    editável por nenhum. Para adaptá-lo, clone.
                    """)
    public ResponseEntity<Page<ModeloFichaResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean doSistema) {
        return ResponseEntity.ok(modeloFichaService.getAll(pageable, nome, ativo, doSistema));
    }

    @GetMapping("/select")
    @Operation(summary = "Modelos ativos para combo",
            description = """
                    Leva `doSistema` e a contagem de perguntas junto: quem escolhe
                    precisa distinguir o modelo do sistema do que a própria
                    clínica montou, e saber o tamanho da ficha que vai abrir.
                    """)
    public ResponseEntity<List<ModeloFichaSelectDto>> select(
            @RequestParam(required = false) String termo) {
        return ResponseEntity.ok(modeloFichaService.select(termo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um modelo, com as suas perguntas em ordem")
    public ResponseEntity<ModeloFichaResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(modeloFichaService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cria um modelo de ficha",
            description = """
                    As perguntas vêm na mesma carga. O modelo criado é **sempre do
                    tenant logado** — não há como criar modelo do sistema pela
                    API, e é isso que mantém os três semeados iguais para todo
                    cliente.
                    """)
    public ResponseEntity<ModeloFichaResponseDto> create(
            @Valid @RequestBody ModeloFichaCreateDto dto) {
        ModeloFichaResponseDto criado = modeloFichaService.create(dto);
        return ResponseEntity.created(URI.create("/modelos-ficha/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um modelo do cliente",
            description = """
                    A lista de perguntas é o **estado completo**: item com `id`
                    atualiza, sem `id` cria, e o que não veio é removido.

                    Não há `ordem` no corpo — ela é a posição na lista, e o
                    servidor a reatribui.

                    Modelo do sistema responde **404**: ele nem é encontrado para
                    alteração.
                    """)
    public ResponseEntity<ModeloFichaResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody ModeloFichaUpdateDto dto) {
        return ResponseEntity.ok(modeloFichaService.update(id, dto));
    }

    @PostMapping("/{id}/clonar")
    @Operation(summary = "Clona um modelo, criando uma cópia do cliente",
            description = """
                    É como se parte de um modelo do sistema sem poder estragá-lo:
                    a cópia nasce do tenant, com as mesmas perguntas, e a partir
                    daí é editável, inativável e apagável.

                    O nome ganha o sufixo `(cópia)` até ficar livre.
                    """)
    public ResponseEntity<ModeloFichaResponseDto> clonar(@PathVariable UUID id) {
        ModeloFichaResponseDto copia = modeloFichaService.clonar(id);
        return ResponseEntity.created(URI.create("/modelos-ficha/" + copia.id())).body(copia);
    }

    @PatchMapping("/{id}/ativo")
    @Operation(summary = "Ativa ou inativa um modelo do cliente")
    public ResponseEntity<ModeloFichaResponseDto> alterarAtivo(
            @PathVariable UUID id, @RequestParam boolean ativo) {
        return ResponseEntity.ok(modeloFichaService.alterarAtivo(id, ativo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um modelo do cliente",
            description = """
                    **Não danifica ficha nenhuma.** A ficha guarda o nome do
                    modelo e o texto integral de cada pergunta respondida, então
                    ela continua legível, imprimível e exportável — o vínculo é
                    que fica nulo.

                    Modelo do sistema responde **404**.
                    """)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        modeloFichaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
