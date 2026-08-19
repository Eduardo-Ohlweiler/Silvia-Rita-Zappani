package com.nutri.hospitalar.pediatria.dtos;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Visão gerencial da pediatria no período.
 *
 * <p>Todas as médias ignoram avaliação em que o valor não existe — média de IMC
 * não conta quem não tinha estatura. Contar como zero puxaria a média para
 * baixo e faria a clínica parecer pior do que é.
 *
 * @param percImcAdequado percentual das avaliações COM classificação de IMC que
 *                        caíram na faixa adequada. No eroERP isso era medido
 *                        procurando "adequado" dentro do texto — com o enum a
 *                        conta é exata.
 */
public record DashboardGeralDto(
        long totalAvaliacoes,
        long totalPacientes,
        long avaliacoesMes,

        BigDecimal idadeMediaMeses,
        BigDecimal pesoMedio,
        BigDecimal imcMedio,
        BigDecimal percImcAdequado,
        BigDecimal coberturaCaloricaMedia,

        /** Série mensal contínua — mês sem avaliação aparece com zero. */
        List<PontoPeriodoDto> porPeriodo,

        List<ContagemFaixaDto> classifPesoIdade,
        List<ContagemFaixaDto> classifEstaturaIdade,
        List<ContagemFaixaDto> classifImcIdade,

        List<ContagemDto> porFormula,
        List<ContagemDto> porFaixaEtaria,
        List<ContagemDto> porSexo,

        List<PacienteRankingDto> pacientesMaisAvaliados
) {

    public record PontoPeriodoDto(String periodo, long avaliacoes) {}

    /** Distribuição por faixa da OMS. `faixa` nula = não classificado. */
    public record ContagemFaixaDto(FaixaOms faixa, String rotulo, long quantidade) {}

    public record ContagemDto(String rotulo, long quantidade) {}

    public record PacienteRankingDto(UUID pacienteId, String pacienteNome, long avaliacoes) {}
}
