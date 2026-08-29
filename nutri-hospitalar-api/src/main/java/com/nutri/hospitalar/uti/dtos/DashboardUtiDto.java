package com.nutri.hospitalar.uti.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Visão gerencial da terapia nutricional de UTI no período.
 *
 * <p>Como no painel da pediatria, <b>toda média ignora a avaliação em que o
 * valor não existe</b>: a média de IMC não conta quem não tinha altura. Contar
 * como zero puxaria a média para baixo e faria o serviço parecer pior do que é.
 *
 * <p>As distribuições saem dos <b>rótulos gravados</b> na avaliação, não de
 * reclassificação: a avaliação de março continua contando na faixa em que foi
 * classificada em março. O tom acompanha o rótulo para o gráfico poder colorir
 * sem interpretar texto.
 *
 * @param adesaoMedia   média do percentual recebido nos dias do período. Ver a
 *                      ressalva de {@link PainelAcompanhamentoUtiDto#adesaoMedia}:
 *                      não é nota de desempenho
 * @param porPeriodo    série mensal <b>contínua</b> — mês sem avaliação aparece
 *                      com zero, senão o gráfico encurta o intervalo e insinua
 *                      atividade que não houve
 */
public record DashboardUtiDto(
        long totalAvaliacoes,
        long totalPacientes,
        long avaliacoesMes,
        long totalDiasRegistrados,

        BigDecimal idadeMediaAnos,
        BigDecimal pesoTrabalhoMedio,
        BigDecimal imcMedio,
        BigDecimal metaEnergeticaMedia,
        BigDecimal metaProteicaMedia,
        BigDecimal kcalPorQuiloMedio,
        BigDecimal proteinaPorQuiloMedio,
        BigDecimal adesaoMedia,
        /** Percentual das avaliações classificadas que caíram em eutrofia. */
        BigDecimal percEutrofia,
        /** Quantas avaliações aplicaram a correção de obesidade da ASPEN. */
        long avaliacoesObesidade,

        List<PontoPeriodoDto> porPeriodo,

        List<ContagemRotuladaDto> classifImcOms,
        List<ContagemRotuladaDto> classifAdequacaoCb,
        List<ContagemRotuladaDto> classifPerdaPeso,

        List<ContagemDto> porFormula,
        List<ContagemDto> porFase,
        List<ContagemDto> porTerapiaRenal,
        List<ContagemDto> porModoInfusao,

        /**
         * Faixa etária do adulto, em degraus com ordem — por isso vai em coluna
         * com rampa ordinal, e não em barra de cor única.
         *
         * <p>O corte em <b>60 anos</b> não é arbitrário: é onde passa a valer a
         * classificação de IMC da OPAS 2002, que usa outros pontos que a da OMS.
         * Uma UTI com metade dos pacientes acima de 60 lê o IMC médio de outro
         * jeito.
         */
        List<ContagemDto> porFaixaEtaria,

        List<PacienteRankingDto> pacientesMaisAvaliados
) {

    /**
     * @param adesaoMedia média do percentual recebido nos dias daquele mês, ou
     *                    {@code null} quando o mês não teve dia com prescrição.
     *                    Nulo e zero são coisas diferentes aqui: zero seria um
     *                    mês em que ninguém recebeu nada
     */
    public record PontoPeriodoDto(String periodo, long avaliacoes, long dias,
                                  BigDecimal adesaoMedia) {}

    /**
     * Distribuição por classificação clínica.
     *
     * @param rotulo texto gravado na avaliação; {@code null} quando não houve
     *               classificação — e a tela escreve "não classificado", não um
     *               traço mudo
     * @param tom    o tom gravado junto, que é por onde o gráfico colore
     */
    public record ContagemRotuladaDto(String rotulo, String tom, long quantidade) {}

    public record ContagemDto(String rotulo, long quantidade) {}

    public record PacienteRankingDto(UUID pacienteId, String pacienteNome,
                                     long avaliacoes, long dias) {}
}
