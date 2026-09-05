package com.nutri.hospitalar.inicio.controller;

import com.nutri.hospitalar.inicio.dtos.PainelInicialDto;
import com.nutri.hospitalar.inicio.service.PainelInicialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tela inicial. Um endpoint só, porque ela serve os dois módulos e pendurá-la
 * em qualquer um deles seria arbitrário.
 *
 * <p>{@code isAuthenticated()} e não role: quem opera dentro do tenant precisa
 * ver a própria lista de trabalho. O isolamento vem do {@code tenantId} da
 * sessão, em toda consulta.
 */
@RestController
@RequestMapping("/painel-inicial")
@RequiredArgsConstructor
@Tag(name = "Tela inicial")
@PreAuthorize("isAuthenticated()")
public class PainelInicialController {

    private final PainelInicialService painelInicialService;

    @GetMapping
    @Operation(summary = "A lista de trabalho do dia",
            description = """
                    Responde "o que preciso fazer hoje", não "como foi o
                    período" — para isso existem as telas em números.

                    Traz quem está em acompanhamento e ainda não tem registro de
                    hoje, os alertas de adesão (só depois da primeira semana de
                    terapia, como a ESPEN manda), as crianças que mudaram de
                    faixa da OMS, e há quanto tempo cada paciente não é avaliado
                    — este último **sem limite e sem cor**, porque não existe
                    regra de "reavaliar a cada N dias" neste sistema.

                    Paciente com acompanhamento **encerrado** não aparece: é essa
                    a saída da lista.
                    """)
    public ResponseEntity<PainelInicialDto> painelInicial() {
        return ResponseEntity.ok(painelInicialService.montar());
    }
}
