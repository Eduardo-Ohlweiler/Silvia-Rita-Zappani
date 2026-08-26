package com.nutri.hospitalar.uti.controller;

import com.nutri.hospitalar.uti.dtos.AvaliacaoSugeridaDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiCreateDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiListaDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiResponseDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiUpdateDto;
import com.nutri.hospitalar.uti.service.RegistroDiarioUtiService;
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
 * A rota literal {@code /avaliacao-sugerida} vem antes de {@code /{id}} — o
 * Spring prefere o literal, e a armadilha está registrada no {@code CLAUDE.md}.
 */
@RestController
@RequestMapping("/uti/registros-diarios")
@RequiredArgsConstructor
@Tag(name = "Acompanhamento diário de UTI")
@PreAuthorize("isAuthenticated()")
public class RegistroDiarioUtiController {

    private final RegistroDiarioUtiService registroDiarioUtiService;

    @GetMapping
    @Operation(summary = "Lista os dias de acompanhamento",
            description = """
                    O percentual recebido vem **derivado**, contra o volume
                    prescrito na avaliação vinculada. Não existe campo digitável
                    de percentual — no eroERP ele existe ao lado de um calculado,
                    e os dois vão para o banco.
                    """)
    public ResponseEntity<Page<RegistroDiarioUtiListaDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(required = false) String pessoaNome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        return ResponseEntity.ok(
                registroDiarioUtiService.getAll(pageable, pessoaId, pessoaNome, de, ate));
    }

    @GetMapping("/avaliacao-sugerida")
    @Operation(summary = "Qual avaliação este dia deveria referenciar",
            description = """
                    A mais recente daquele paciente **até** aquela data — um dia
                    de três meses atrás não deve ser comparado com a prescrição
                    de ontem.

                    É **sugestão**, não vínculo: a tela mostra qual seria e o
                    usuário confirma ou troca. Ligar em silêncio faria o kcal/kg
                    mudar sem que ninguém tivesse escolhido a referência.

                    Paciente sem avaliação até a data devolve o campo vazio e o
                    motivo — o dia pode ser registrado assim mesmo.
                    """)
    public ResponseEntity<AvaliacaoSugeridaDto> avaliacaoSugerida(
            @RequestParam UUID pessoaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(registroDiarioUtiService.avaliacaoSugerida(pessoaId, data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abre um dia de acompanhamento")
    public ResponseEntity<RegistroDiarioUtiResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(registroDiarioUtiService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Registra um dia",
            description = """
                    **Um registro por paciente por dia.** Repetir devolve 409
                    dizendo qual dia já existe, para o usuário abrir o registro
                    em vez de criar outro.

                    O vínculo com a avaliação é opcional: paciente que internou
                    de madrugada tem dia antes de avaliação. Sem ele, kcal/kg,
                    proteína por quilo e diurese por quilo ficam de fora — e o
                    motivo acompanha a resposta.
                    """)
    public ResponseEntity<RegistroDiarioUtiResponseDto> create(
            @Valid @RequestBody RegistroDiarioUtiCreateDto dto) {
        RegistroDiarioUtiResponseDto criado = registroDiarioUtiService.create(dto);
        return ResponseEntity.created(
                URI.create("/uti/registros-diarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um dia de acompanhamento")
    public ResponseEntity<RegistroDiarioUtiResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody RegistroDiarioUtiUpdateDto dto) {
        return ResponseEntity.ok(registroDiarioUtiService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um dia de acompanhamento")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        registroDiarioUtiService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
