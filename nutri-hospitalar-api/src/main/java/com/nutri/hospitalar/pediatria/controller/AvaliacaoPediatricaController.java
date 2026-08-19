package com.nutri.hospitalar.pediatria.controller;

import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaCreateDto;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaListaDto;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaResponseDto;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaUpdateDto;
import com.nutri.hospitalar.pediatria.dtos.CalculoPediatricoRequestDto;
import com.nutri.hospitalar.pediatria.dtos.ResultadoPediatricoDto;
import com.nutri.hospitalar.pediatria.service.AvaliacaoPediatricaService;
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
@RequestMapping("/pediatria/avaliacoes")
@RequiredArgsConstructor
@Tag(name = "Avaliação pediátrica")
@PreAuthorize("isAuthenticated()")
public class AvaliacaoPediatricaController {

    private final AvaliacaoPediatricaService avaliacaoService;

    /*
     * Rota literal ANTES de /{id}: o Spring prefere o literal, mas manter a
     * ordem no arquivo evita a dúvida. Se der "valor inválido para o parâmetro
     * id", a aplicação em execução está desatualizada — reinicie.
     */
    @PostMapping("/calcular")
    @Operation(summary = "Calcula sem gravar",
            description = """
                    É o que a tela chama a cada alteração de campo. Nada é
                    obrigatório: faltando uma entrada, o resultado que dependia
                    dela vem ausente **com o motivo**, e não com um 400 que
                    apagaria a tela a cada tecla.

                    Mesma conta que a gravação usa — é impossível a tela mostrar
                    um número e o banco guardar outro.
                    """)
    public ResponseEntity<ResultadoPediatricoDto> calcular(
            @Valid @RequestBody CalculoPediatricoRequestDto dto) {
        return ResponseEntity.ok(avaliacaoService.calcular(dto));
    }

    @GetMapping
    @Operation(summary = "Lista as avaliações do cliente",
            description = "Ordenação fixa: data mais recente primeiro, depois nome do paciente.")
    public ResponseEntity<Page<AvaliacaoPediatricaListaDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) UUID formulaLacteaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Integer mesesMin,
            @RequestParam(required = false) Integer mesesMax) {
        return ResponseEntity.ok(avaliacaoService.getAll(
                pageable, pacienteId, formulaLacteaId, de, ate, mesesMin, mesesMax));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abre uma avaliação salva",
            description = """
                    Devolve o resultado **como foi gravado**, sem recalcular. Se
                    as curvas da OMS ou as DRIs mudarem, o registro continua
                    mostrando o que se decidiu na época.
                    """)
    public ResponseEntity<AvaliacaoPediatricaResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(avaliacaoService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Grava uma avaliação",
            description = """
                    Aceita **só entradas**. O servidor recalcula e grava o
                    resultado do próprio cálculo — nenhum resultado enviado pelo
                    cliente é considerado.
                    """)
    public ResponseEntity<AvaliacaoPediatricaResponseDto> create(
            @Valid @RequestBody AvaliacaoPediatricaCreateDto dto) {
        AvaliacaoPediatricaResponseDto criada = avaliacaoService.create(dto);
        return ResponseEntity.created(URI.create("/pediatria/avaliacoes/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera uma avaliação",
            description = "Alterar qualquer entrada refaz o cálculo inteiro no servidor.")
    public ResponseEntity<AvaliacaoPediatricaResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody AvaliacaoPediatricaUpdateDto dto) {
        return ResponseEntity.ok(avaliacaoService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma avaliação")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        avaliacaoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
