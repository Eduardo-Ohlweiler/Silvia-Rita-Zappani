package com.nutri.hospitalar.uti.controller;

import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiCreateDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiFiltrosDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiListaDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiResponseDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiUpdateDto;
import com.nutri.hospitalar.uti.service.AvaliacaoUtiService;
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

@RestController
@RequestMapping("/uti/avaliacoes")
@RequiredArgsConstructor
@Tag(name = "Avaliações de UTI adulto")
@PreAuthorize("isAuthenticated()")
public class AvaliacaoUtiController {

    private final AvaliacaoUtiService avaliacaoUtiService;

    @GetMapping
    @Operation(summary = "Lista as avaliações do cliente",
            description = """
                    Traz o mínimo para decidir qual abrir: paciente, data, peso
                    considerado com a origem, IMC classificado e a meta que estava
                    prescrita. A classificação vem **gravada**, não recalculada.
                    """)
    public ResponseEntity<Page<AvaliacaoUtiListaDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) String pacienteNome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        return ResponseEntity.ok(avaliacaoUtiService.getAll(pageable,
                new AvaliacaoUtiFiltrosDto(pacienteId, pacienteNome, de, ate)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abre uma avaliação gravada",
            description = """
                    **Não recalcula.** Devolve as entradas, para repovoar o
                    formulário, e os resultados como foram gravados no dia. Se as
                    equações mudarem, este registro não muda — prontuário não se
                    reescreve sozinho.

                    A progressão da dieta e a distribuição da água vêm vazias:
                    são tabelas derivadas e não fazem parte do registro. O motivo
                    acompanha.
                    """)
    public ResponseEntity<AvaliacaoUtiResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(avaliacaoUtiService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Grava uma avaliação",
            description = """
                    O corpo leva **só entradas**, no mesmo formato que
                    `POST /uti/calculo` recebe — é literalmente o mesmo DTO
                    aninhado em `calculo`. O servidor recalcula e grava os seus
                    próprios números; resultado mandado no corpo é ignorado.

                    Junto vai o **retrato da fórmula**, macros inclusive: a
                    fórmula pode sair do catálogo depois, e a avaliação precisa
                    continuar legível.
                    """)
    public ResponseEntity<AvaliacaoUtiResponseDto> create(
            @Valid @RequestBody AvaliacaoUtiCreateDto dto) {
        AvaliacaoUtiResponseDto criada = avaliacaoUtiService.create(dto);
        return ResponseEntity.created(URI.create("/uti/avaliacoes/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera uma avaliação",
            description = """
                    Recalcula a partir das entradas novas e regrava tudo,
                    inclusive o retrato da fórmula. Avaliação de outro cliente
                    devolve 404.
                    """)
    public ResponseEntity<AvaliacaoUtiResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody AvaliacaoUtiUpdateDto dto) {
        return ResponseEntity.ok(avaliacaoUtiService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma avaliação")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        avaliacaoUtiService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
