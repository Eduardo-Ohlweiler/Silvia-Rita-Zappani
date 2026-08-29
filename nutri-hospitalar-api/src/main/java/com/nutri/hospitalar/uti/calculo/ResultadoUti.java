package com.nutri.hospitalar.uti.calculo;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que as quatro abas exibem, agrupado como elas.
 *
 * <p>Todo valor pode faltar, e <b>quando falta vem o motivo</b>: nunca um traço
 * mudo. É o que permite à tela dizer "informe a altura do joelho para estimar a
 * altura" em vez de deixar o profissional adivinhar por que o campo está vazio.
 *
 * <p>Os valores da cascata trazem a <b>origem por escrito</b> — "peso estimado ·
 * Rabito 2008", "da faixa da fase" — porque prescrição sem procedência não é
 * auditável.
 */
public record ResultadoUti(
        Antropometria antropometria,
        Necessidades necessidades,
        Dieta dieta,
        Hidratacao hidratacao
) {

    /**
     * @param pesoDeTrabalho o peso que TODO o resto usou, e de onde ele veio
     * @param alturaUsada    idem para a altura
     */
    public record Antropometria(
            BigDecimal alturaEstimadaCm,
            BigDecimal pesoChumleaKg,
            BigDecimal pesoJungKg,
            BigDecimal pesoRabitoKg,
            String motivoEstimativas,

            BigDecimal pesoDeTrabalhoKg,
            String pesoDeTrabalhoOrigem,
            BigDecimal alturaUsadaCm,
            String alturaUsadaOrigem,
            String motivoPesoDeTrabalho,

            BigDecimal imc,
            Classificacao classificacaoImcOms,
            Classificacao classificacaoImcOpas,
            String motivoImc,

            BigDecimal pesoIdealKg,
            BigDecimal pesoIdealImc25Kg,
            BigDecimal pesoAjustadoKg,
            BigDecimal pesoCorrigidoAmputacaoKg,

            BigDecimal percentualPerdaPeso,
            Classificacao classificacaoPerdaPeso,
            String motivoPerdaPeso,

            BigDecimal p50CircBracoCm,
            BigDecimal adequacaoCircBracoPerc,
            Classificacao classificacaoAdequacaoCircBraco,
            String motivoAdequacaoCircBraco,

            BigDecimal circBracoAjustadaCm,
            Classificacao classificacaoMassaMuscularBraco,
            BigDecimal circPanturrilhaAjustadaCm,
            Classificacao classificacaoDeplecaoPanturrilha,
            String populacaoReferenciaUsada,
            boolean ajustePeloImcRelevante,
            String motivoDeplecao
    ) {}

    /**
     * @param obeso  quando verdadeiro, a fase da terapia <b>não se aplica</b> —
     *               o eroERP a ignora em silêncio nesse ramo
     * @param baseDoPeso qual peso a energia usou: atual até IMC 40, ideal daí em
     *                   diante. É o erro mais fácil de cometer, e por isso vem
     *                   escrito
     */
    public record Necessidades(
            BigDecimal energiaMinima,
            BigDecimal energiaMaxima,
            BigDecimal proteinaMinima,
            BigDecimal proteinaMaxima,

            BigDecimal metaEnergetica,
            String metaEnergeticaOrigem,
            BigDecimal metaProteica,
            String metaProteicaOrigem,

            BigDecimal proteinaTerapiaRenal,
            boolean obeso,
            String baseDoPeso,
            String motivo
    ) {}

    /**
     * @param formulaNome       retrato do que foi usado, para a tela e para o
     *                          registro
     * @param progressao        a escada de 25 a 100 % dos dias 1 a 4
     * @param motivoProgressao  por que a escada está vazia, quando está. Tem
     *                          campo próprio porque a tabela pode faltar com o
     *                          bloco inteiro calculado — é o caso da avaliação
     *                          salva, que não grava tabela derivada. Sem isto o
     *                          motivo da tabela ocupava o {@code motivo} do
     *                          bloco e aparecia colado a números que existem
     * @param motivo            por que o BLOCO não saiu
     */
    public record Dieta(
            String formulaNome,
            BigDecimal densidadeKcalMl,
            BigDecimal proteinaGL,

            BigDecimal volumeTotalMl,
            String volumeTotalDescricao,
            BigDecimal caloriasOfertadas,
            BigDecimal proteinaOfertada,
            BigDecimal caloriasPorQuilo,
            BigDecimal proteinaPorQuilo,
            BigDecimal percentualDoVct,
            BigDecimal percentualDaProteina,

            BigDecimal choOfertado,
            BigDecimal lipOfertado,
            BigDecimal fibrasOfertadas,
            BigDecimal potassioOfertado,

            BigDecimal volumePleno,
            BigDecimal proteinaNoVolumePleno,
            BigDecimal proteinaSuplementar,
            String unidadeDoVolume,

            List<DegrauProgressao> progressao,
            String motivoProgressao,
            String motivo
    ) {}

    public record DegrauProgressao(int dia, BigDecimal percentual, BigDecimal kcal,
                                   BigDecimal volume) {}

    /**
     * @param percentualAguaOrigem  "água livre do rótulo" ou "estimada pela
     *                              densidade" — a diferença importa, e some se
     *                              não for dita
     * @param motivoDistribuicao    por que as duas distribuições estão vazias.
     *                              Campo próprio pelo mesmo motivo de
     *                              {@link Dieta#motivoProgressao}
     */
    public record Hidratacao(
            BigDecimal necessidadeMinima,
            BigDecimal necessidadeIdeal,

            BigDecimal percentualAgua,
            String percentualAguaOrigem,
            BigDecimal volumeDietaConsiderado,
            BigDecimal aguaNaDieta,

            BigDecimal aguaExtraMinima,
            BigDecimal aguaExtraIdeal,
            List<FracaoAgua> distribuicaoMinima,
            List<FracaoAgua> distribuicaoIdeal,
            String motivoDistribuicao,
            String motivo
    ) {}

    public record FracaoAgua(int vezesAoDia, BigDecimal mlPorVez) {}
}
