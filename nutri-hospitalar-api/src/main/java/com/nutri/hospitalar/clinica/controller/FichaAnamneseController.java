package com.nutri.hospitalar.clinica.controller;

import com.nutri.hospitalar.clinica.dtos.FichaAnamneseCreateDto;
import com.nutri.hospitalar.clinica.dtos.FichaAnamneseListaDto;
import com.nutri.hospitalar.clinica.dtos.FichaAnamneseResponseDto;
import com.nutri.hospitalar.clinica.dtos.FichaAnamneseUpdateDto;
import com.nutri.hospitalar.clinica.service.FichaAnamneseService;
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
 * Fichas de anamnese preenchidas.
 *
 * <p>Módulo de negócio: {@code isAuthenticated()} — {@code ADMIN} e {@code USER}
 * operam, e o isolamento vem do tenant efetivo do token.
 */
@RestController
@RequestMapping("/fichas-anamnese")
@RequiredArgsConstructor
@Tag(name = "Fichas de anamnese")
@PreAuthorize("isAuthenticated()")
public class FichaAnamneseController {

    private final FichaAnamneseService fichaAnamneseService;

    @GetMapping
    @Operation(summary = "Lista as fichas de anamnese",
            description = """
                    O `modeloNome` de cada linha vem do **retrato** gravado na
                    ficha, e não do catálogo: a lista continua dizendo de qual
                    modelo cada ficha veio mesmo depois de ele ser renomeado ou
                    apagado.

                    `respondidas` de `totalPerguntas` distingue a ficha completa
                    da que ficou pela metade, sem precisar abrir.
                    """)
    public ResponseEntity<Page<FichaAnamneseListaDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) UUID modeloId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(fichaAnamneseService.getAll(pageable, pacienteId, modeloId, de, ate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abre a ficha como ela foi gravada",
            description = """
                    Cada resposta traz o **retrato da pergunta** — seção, rótulo,
                    tipo, opções, ordem e obrigatoriedade —, e é dele que a tela e
                    o papel desenham a ficha. O modelo de hoje não é consultado
                    para isso.

                    Dois avisos vêm junto, e são conversas diferentes:
                    `modeloRemovido` é "o modelo não existe mais, e esta ficha
                    continua inteira"; `modeloAlterado` é "o modelo ainda existe,
                    mas não é mais este". O segundo engana mais, porque o nome
                    continua o mesmo.
                    """)
    public ResponseEntity<FichaAnamneseResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(fichaAnamneseService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Grava uma ficha de anamnese",
            description = """
                    Chegam apenas `campoId` e `valor` por resposta: o retrato da
                    pergunta é copiado pelo servidor a partir do modelo, e nunca
                    vem do cliente — aceitar rótulo do front abriria caminho para
                    uma ficha afirmar ter perguntado o que o modelo não perguntava.

                    `valor` é sempre texto. Sim/não manda `"true"`, `"false"` ou
                    **nulo**, e nulo é *não informado*, que não é a mesma coisa
                    que *não*.
                    """)
    public ResponseEntity<FichaAnamneseResponseDto> create(
            @Valid @RequestBody FichaAnamneseCreateDto dto) {
        FichaAnamneseResponseDto criada = fichaAnamneseService.create(dto);
        return ResponseEntity.created(URI.create("/fichas-anamnese/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera uma ficha de anamnese",
            description = """
                    Regravar **atualiza o retrato** a partir das perguntas de
                    hoje: é o mesmo encontro sendo corrigido, com quem corrige
                    vendo o que o modelo pergunta agora. O que nunca acontece é o
                    retrato mudar sem alguém regravar.

                    Trocar o `modeloId` descarta as respostas antigas e recomeça.
                    """)
    public ResponseEntity<FichaAnamneseResponseDto> update(
            @PathVariable UUID id, @Valid @RequestBody FichaAnamneseUpdateDto dto) {
        return ResponseEntity.ok(fichaAnamneseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma ficha de anamnese")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fichaAnamneseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
