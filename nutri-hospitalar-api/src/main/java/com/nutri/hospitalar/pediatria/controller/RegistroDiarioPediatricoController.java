package com.nutri.hospitalar.pediatria.controller;

import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaSugeridaDto;
import com.nutri.hospitalar.pediatria.dtos.PainelAcompanhamentoPediatricoDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoCreateDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoListaDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoResponseDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoUpdateDto;
import com.nutri.hospitalar.pediatria.service.RegistroDiarioPediatricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

/**
 * O acompanhamento diário pediátrico. Especificação em {@code docs/11}.
 *
 * <p>A rota literal {@code /avaliacao-sugerida} vem antes de {@code /{id}} — o
 * Spring prefere o literal, e a armadilha está registrada no {@code CLAUDE.md}.
 */
@RestController
@RequestMapping("/pediatria/registros-diarios")
@RequiredArgsConstructor
@Tag(name = "Acompanhamento diário pediátrico")
@PreAuthorize("isAuthenticated()")
public class RegistroDiarioPediatricoController {

    private final RegistroDiarioPediatricoService registroDiarioPediatricoService;

    @GetMapping
    @Operation(summary = "Lista os dias de acompanhamento",
            description = """
                    Tudo o que não é entrada vem **derivado**: o percentual
                    recebido contra o volume prescrito na avaliação vinculada, as
                    adequações contra o VET e a DRI, e a idade calculada da data
                    de nascimento — na pediatria a idade anda, e dois registros
                    do mesmo paciente com 30 dias de intervalo são idades
                    diferentes.
                    """)
    public ResponseEntity<Page<RegistroDiarioPediatricoListaDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(required = false) String pessoaNome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(
                registroDiarioPediatricoService.getAll(pageable, pessoaId, pessoaNome, de, ate));
    }

    @GetMapping("/avaliacao-sugerida")
    @Operation(summary = "Qual avaliação o dia deveria referenciar",
            description = """
                    **Sugestão, não vínculo.** A mais recente daquele paciente
                    *até* aquela data — um dia de três meses atrás não deve ser
                    comparado com a prescrição de ontem. A tela mostra e o
                    usuário confirma: ligar sozinho faria a adequação calórica
                    mudar sem que ninguém tivesse escolhido a referência.
                    """)
    public ResponseEntity<AvaliacaoPediatricaSugeridaDto> avaliacaoSugerida(
            @RequestParam UUID pessoaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(
                registroDiarioPediatricoService.avaliacaoSugerida(pessoaId, data));
    }

    @GetMapping("/painel")
    @Operation(summary = "O painel de acompanhamento de um paciente",
            description = """
                    Os dias de uma criança no período: como ela cresceu, o que
                    recebeu e quanto disso cobriu a necessidade.

                    **A janela pedida é respeitada**, não os dias que têm dado —
                    onze meses de zero também são informação. E a variação de
                    peso é entre o primeiro e o último dia *que têm peso
                    medido*: um dia sem balança no fim da janela zeraria o ganho
                    de duas semanas.
                    """)
    public ResponseEntity<PainelAcompanhamentoPediatricoDto> painel(
            @RequestParam UUID pessoaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(
                registroDiarioPediatricoService.painelAcompanhamento(pessoaId, de, ate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abre um dia de acompanhamento")
    public ResponseEntity<RegistroDiarioPediatricoResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(registroDiarioPediatricoService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Registra um dia",
            description = """
                    Um registro por paciente por dia: o segundo devolve **409**
                    dizendo qual dia já existe. E aceitar mais tomadas do que se
                    previu também devolve 409 — passaria calado como adequação
                    acima de 100 %.
                    """)
    public ResponseEntity<RegistroDiarioPediatricoResponseDto> create(
            @Valid @RequestBody RegistroDiarioPediatricoCreateDto dto) {
        RegistroDiarioPediatricoResponseDto criado = registroDiarioPediatricoService.create(dto);
        return ResponseEntity.created(
                URI.create("/pediatria/registros-diarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um dia de acompanhamento")
    public ResponseEntity<RegistroDiarioPediatricoResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody RegistroDiarioPediatricoUpdateDto dto) {
        return ResponseEntity.ok(registroDiarioPediatricoService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um dia de acompanhamento")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        registroDiarioPediatricoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
