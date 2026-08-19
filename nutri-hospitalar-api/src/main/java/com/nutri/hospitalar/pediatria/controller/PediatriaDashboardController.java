package com.nutri.hospitalar.pediatria.controller;

import com.nutri.hospitalar.pediatria.dtos.CurvaOmsPontoDto;
import com.nutri.hospitalar.pediatria.dtos.DashboardGeralDto;
import com.nutri.hospitalar.pediatria.dtos.PainelPacienteDto;
import com.nutri.hospitalar.pediatria.service.PediatriaDashboardService;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pediatria")
@RequiredArgsConstructor
@Tag(name = "Painéis da pediatria")
@PreAuthorize("isAuthenticated()")
public class PediatriaDashboardController {

    private final PediatriaDashboardService dashboardService;

    @GetMapping("/curvas-oms")
    @Operation(summary = "Curva de crescimento da OMS para desenhar o gráfico",
            description = """
                    Os cinco percentis por mês, no intervalo pedido. Dado de
                    referência internacional, igual para todo cliente.

                    A tela desenha a faixa P3–P97 clara por baixo e a P15–P85
                    escura por cima — só a segunda classifica; a primeira situa
                    o extremo, como na curva impressa da OMS.
                    """)
    public ResponseEntity<List<CurvaOmsPontoDto>> curva(
            @RequestParam Sexo sexo,
            @RequestParam(required = false) Integer idadeMin,
            @RequestParam(required = false) Integer idadeMax) {
        return ResponseEntity.ok(dashboardService.curva(sexo, idadeMin, idadeMax));
    }

    @GetMapping("/painel-paciente")
    @Operation(summary = "Acompanhamento de uma criança no tempo",
            description = """
                    Onde a criança está hoje e por onde andou: última avaliação,
                    trajetória para as curvas de crescimento e histórico de
                    fórmulas.

                    `dias = 0` traz desde sempre. Nada é recalculado — são os
                    números gravados em cada avaliação.
                    """)
    public ResponseEntity<PainelPacienteDto> painelPaciente(
            @RequestParam UUID pacienteId,
            @RequestParam(defaultValue = "0") int dias,
            @RequestParam(required = false) UUID formulaLacteaId,
            @RequestParam(required = false) Integer mesesMin,
            @RequestParam(required = false) Integer mesesMax) {
        return ResponseEntity.ok(
                dashboardService.painelPaciente(pacienteId, dias, formulaLacteaId, mesesMin, mesesMax));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Visão gerencial da pediatria no período",
            description = """
                    Indicadores, distribuições e ranking do cliente. `dias = 0`
                    traz desde sempre.

                    As médias ignoram avaliação sem o valor: a média de IMC não
                    conta quem não tinha estatura.
                    """)
    public ResponseEntity<DashboardGeralDto> dashboardGeral(
            @RequestParam(defaultValue = "365") int dias,
            @RequestParam(required = false) UUID formulaLacteaId,
            @RequestParam(required = false) Integer mesesMin,
            @RequestParam(required = false) Integer mesesMax,
            @RequestParam(required = false) Sexo sexo) {
        return ResponseEntity.ok(
                dashboardService.dashboardGeral(dias, formulaLacteaId, mesesMin, mesesMax, sexo));
    }
}
