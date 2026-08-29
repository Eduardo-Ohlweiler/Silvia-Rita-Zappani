package com.nutri.hospitalar.uti.controller;

import com.nutri.hospitalar.uti.dtos.DashboardUtiDto;
import com.nutri.hospitalar.uti.dtos.PainelAcompanhamentoUtiDto;
import com.nutri.hospitalar.uti.dtos.PainelPacienteUtiDto;
import com.nutri.hospitalar.uti.service.UtiDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Os três painéis da UTI adulto. Só leitura, e só agregação — nenhum destes
 * endpoints recalcula nutrição.
 *
 * <p>{@code isAuthenticated()} e não role: é módulo de negócio, e quem opera
 * dentro do tenant precisa ver o próprio painel. O isolamento vem do
 * {@code tenantId} da sessão, em toda consulta.
 */
@RestController
@RequestMapping("/uti")
@RequiredArgsConstructor
@Tag(name = "Painéis da UTI adulto")
@PreAuthorize("isAuthenticated()")
public class UtiDashboardController {

    private final UtiDashboardService dashboardService;

    @GetMapping("/painel-paciente")
    @Operation(summary = "Um paciente no tempo",
            description = """
                    Onde o paciente está hoje e por onde a terapia passou:
                    última avaliação inteira, trajetória de peso, IMC, metas e
                    adesão, e o histórico de fórmulas.

                    `dias = 0` traz desde sempre. Nada é recalculado — cada
                    ponto sai das colunas gravadas na avaliação daquele dia.
                    """)
    public ResponseEntity<PainelPacienteUtiDto> painelPaciente(
            @RequestParam UUID pacienteId,
            @RequestParam(defaultValue = "0") int dias,
            @RequestParam(required = false) UUID formulaEnteralId) {
        return ResponseEntity.ok(
                dashboardService.painelPaciente(pacienteId, dias, formulaEnteralId));
    }

    @GetMapping("/painel-acompanhamento")
    @Operation(summary = "Os dias de acompanhamento de um paciente",
            description = """
                    Adesão à dieta, laboratório, balanço hídrico, diurese e
                    hemodinâmica, dia a dia, em ordem cronológica.

                    Sem `de`, a janela é dos últimos 30 dias. Os dias vêm na
                    mesma forma que a tela do acompanhamento já lê, com os
                    derivados calculados por um único dono.

                    A **adesão não é nota de desempenho**: a ESPEN recomenda
                    oferta abaixo de 70 % nos primeiros dias, então 60 % no dia
                    2 é conduta e no dia 10 é alerta.
                    """)
    public ResponseEntity<PainelAcompanhamentoUtiDto> painelAcompanhamento(
            @RequestParam UUID pessoaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(dashboardService.painelAcompanhamento(pessoaId, de, ate));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Terapia nutricional em números",
            description = """
                    Visão gerencial do período: volume de avaliações e de dias,
                    médias antropométricas e de meta, distribuição das
                    classificações e ranking de pacientes.

                    `dias = 0` traz desde sempre; o padrão são 365 dias. As
                    médias ignoram a avaliação em que o valor não existe —
                    ausência não é zero.
                    """)
    public ResponseEntity<DashboardUtiDto> dashboard(
            @RequestParam(defaultValue = "365") int dias,
            @RequestParam(required = false) UUID formulaEnteralId) {
        return ResponseEntity.ok(dashboardService.dashboard(dias, formulaEnteralId));
    }
}
