package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.pessoa.enums.Sexo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O paciente de UTI no tempo: onde ele está hoje e por onde a terapia passou.
 *
 * <p>Nada aqui é recalculado — cada ponto sai das colunas gravadas na avaliação
 * daquele dia. Reabrir o painel um ano depois mostra a mesma coisa, ainda que a
 * fórmula tenha saído do catálogo ou a régua clínica tenha mudado.
 *
 * <p>O sexo vem de {@code pessoa}, e não da última avaliação, pela mesma razão
 * do painel da pediatria: um paciente sem avaliação nenhuma ainda precisa ser
 * identificado.
 *
 * @param idadeAnosAtual        idade hoje, da data de nascimento. Nula quando o
 *                              cadastro não a tem — e aí a tela mostra a idade
 *                              da última avaliação, que é o que se sabe
 * @param totalDiasRegistrados  quantos dias de acompanhamento existem para este
 *                              paciente. É a ponte para o outro painel: sem
 *                              isso, quem abre este não descobre que há 30 dias
 *                              de laboratório do lado
 */
public record PainelPacienteUtiDto(
        UUID pacienteId,
        String pacienteNome,
        Sexo sexo,
        LocalDate dataNascimento,
        Integer idadeAnosAtual,

        long totalAvaliacoes,
        LocalDate primeiraAvaliacao,
        LocalDate ultimaAvaliacao,

        /** A avaliação mais recente, inteira — a mesma forma que a tela já lê. */
        AvaliacaoUtiResponseDto ultima,

        /** Ordenada por data: é o eixo X de todos os gráficos deste painel. */
        List<PontoAvaliacaoDto> evolucao,

        List<HistoricoFormulaDto> historicoFormulas,

        long totalDiasRegistrados,
        LocalDate primeiroDia,
        LocalDate ultimoDia
) {

    /**
     * Um ponto da trajetória.
     *
     * <p>Rótulo e tom das classificações viajam juntos porque foram gravados
     * juntos: o tom é o que o gráfico usa para colorir, e colorir por texto —
     * como o eroERP fazia com {@code includes('adequado')} — quebra no primeiro
     * rótulo que contém a palavra por acaso.
     */
    public record PontoAvaliacaoDto(
            LocalDate dataAvaliacao,
            Integer idadeAnos,

            BigDecimal pesoTrabalhoKg,
            BigDecimal imc,
            String classifImcOms,
            String classifImcOmsTom,

            BigDecimal percPerdaPeso,
            String classifPerdaPeso,
            String classifPerdaPesoTom,

            BigDecimal adequacaoCircBracoPerc,
            String classifAdequacaoCb,
            String classifAdequacaoCbTom,

            BigDecimal metaEnergetica,
            BigDecimal metaProteica,

            BigDecimal volumeTotalMl,
            BigDecimal caloriasOfertadas,
            BigDecimal proteinaOfertada,
            BigDecimal caloriasPorQuilo,
            BigDecimal proteinaPorQuilo,
            BigDecimal percentualDoVct,
            BigDecimal percentualDaProteina
    ) {}

    /**
     * Por qual fórmula o paciente passou e quando.
     *
     * <p>Sai do retrato gravado na avaliação, não do catálogo: a fórmula pode
     * ter sido desativada, e o histórico continua contando a verdade.
     */
    public record HistoricoFormulaDto(
            String formulaNome,
            long avaliacoes,
            LocalDate primeiroUso,
            LocalDate ultimoUso
    ) {}
}
