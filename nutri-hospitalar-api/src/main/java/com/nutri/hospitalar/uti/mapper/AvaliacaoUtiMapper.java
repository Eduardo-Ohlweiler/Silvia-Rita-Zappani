package com.nutri.hospitalar.uti.mapper;

import com.nutri.hospitalar.uti.calculo.Classificacao;
import com.nutri.hospitalar.uti.calculo.ResultadoUti;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiResponseDto;
import com.nutri.hospitalar.uti.dtos.CalculoUtiRequestDto;
import com.nutri.hospitalar.uti.entity.AvaliacaoUti;
import com.nutri.hospitalar.uti.enums.TomResultado;

import java.util.List;

/**
 * Converte a avaliação gravada de volta na forma que a tela conhece.
 *
 * <p>O ponto delicado é o {@code resultado}: ele é <b>reconstruído das colunas
 * gravadas</b>, não recalculado. Um mapper que chamasse o calculador aqui faria
 * exatamente o que o registro clínico não pode fazer — mudar de valor quando a
 * régua muda.
 *
 * <p>Duas coisas ficam de fora da reconstrução, e é deliberado:
 * a <b>progressão da dieta</b> e a <b>distribuição da água</b> são tabelas
 * derivadas de valores que já estão gravados, e guardá-las linha a linha seria
 * duplicar dado sem ganho. A tela as mostra vazias na avaliação salva, com o
 * motivo — quem quiser vê-las abre a calculadora com as mesmas entradas.
 */
public final class AvaliacaoUtiMapper {

    private AvaliacaoUtiMapper() {}

    private static final String TABELA_NAO_GRAVADA =
            "Tabela derivada — não faz parte do registro. Reproduza na calculadora se precisar.";

    public static AvaliacaoUtiResponseDto toResponse(AvaliacaoUti a) {
        return new AvaliacaoUtiResponseDto(
                a.getId(),
                a.getPaciente().getId(),
                a.getPaciente().getNome(),
                a.getProfissional() == null ? null : a.getProfissional().getId(),
                a.getProfissional() == null ? null : a.getProfissional().getNome(),
                a.getDataAvaliacao(),
                toEntradas(a),
                toResultado(a),
                // A FK é ON DELETE SET NULL: nome gravado sem FK significa que a
                // fórmula saiu do catálogo depois desta avaliação.
                a.getFormulaEnteral() == null && a.getFormulaNome() != null,
                a.getObservacao(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    /** As entradas, do jeito que repovoam o formulário. */
    public static CalculoUtiRequestDto toEntradas(AvaliacaoUti a) {
        return new CalculoUtiRequestDto(
                a.getSexo(), a.getEtnia(), a.getIdadeAnos(),
                a.getAlturaCm(), a.getAlturaJoelhoCm(),
                a.getCircBracoCm(), a.getCircPanturrilhaCm(), a.getCircAbdominalCm(),
                a.getPesoAtualKg(), a.getPesoUsualKg(),
                a.getJanelaPerda(), a.getSegmentosAmputados(),
                a.getPopulacaoReferencia(), a.getOrigemPesoPreferida(),
                a.getFase(), a.getTerapiaRenal(),
                a.getKcalPorKgAlvo(), a.getProteinaPorKgAlvo(),
                a.getPosicaoNaFaixa(),
                a.getFormulaEnteral() == null ? null : a.getFormulaEnteral().getId(),
                a.getModoInfusao(), a.getVolumePorTempo(), a.getTempo(),
                a.getVolumeDietaManualMl());
    }

    /** Os resultados, exatamente como foram gravados. */
    public static ResultadoUti toResultado(AvaliacaoUti a) {
        return new ResultadoUti(
                new ResultadoUti.Antropometria(
                        a.getAlturaEstimadaCm(), a.getPesoChumleaKg(),
                        a.getPesoJungKg(), a.getPesoRabitoKg(), null,

                        a.getPesoTrabalhoKg(), a.getPesoTrabalhoOrigem(),
                        a.getAlturaUsadaCm(), a.getAlturaUsadaOrigem(), null,

                        a.getImc(),
                        classificacao(a.getClassifImcOms(), a.getClassifImcOmsTom()),
                        classificacao(a.getClassifImcOpas(), a.getClassifImcOpasTom()),
                        null,

                        a.getPesoIdealKg(), a.getPesoIdealImc25Kg(),
                        a.getPesoAjustadoKg(), a.getPesoAmputacaoKg(),

                        a.getPercPerdaPeso(),
                        classificacao(a.getClassifPerdaPeso(), a.getClassifPerdaPesoTom()),
                        null,

                        a.getP50CircBracoCm(), a.getAdequacaoCircBracoPerc(),
                        classificacao(a.getClassifAdequacaoCb(), a.getClassifAdequacaoCbTom()),
                        null,

                        a.getCircBracoAjustadaCm(),
                        classificacao(a.getClassifMassaBraco(), a.getClassifMassaBracoTom()),
                        a.getCircPanturrilhaAjustadaCm(),
                        classificacao(a.getClassifDeplecaoCp(), a.getClassifDeplecaoCpTom()),
                        a.getPopulacaoReferencia() == null
                                ? null : a.getPopulacaoReferencia().getDescricao(),
                        false,
                        null),

                new ResultadoUti.Necessidades(
                        a.getEnergiaMinima(), a.getEnergiaMaxima(),
                        a.getProteinaMinima(), a.getProteinaMaxima(),
                        a.getMetaEnergetica(), a.getMetaEnergeticaOrigem(),
                        a.getMetaProteica(), a.getMetaProteicaOrigem(),
                        a.getProteinaTerapiaRenal(),
                        Boolean.TRUE.equals(a.getObeso()), a.getBaseDoPeso(), null),

                new ResultadoUti.Dieta(
                        a.getFormulaNome(), a.getFormulaDensidadeKcalMl(), a.getFormulaProteinaGL(),
                        a.getVolumeTotalMl(), null,
                        a.getCaloriasOfertadas(), a.getProteinaOfertada(),
                        a.getCaloriasPorQuilo(), a.getProteinaPorQuilo(),
                        a.getPercentualDoVct(), a.getPercentualDaProteina(),
                        a.getChoOfertado(), a.getLipOfertado(),
                        a.getFibrasOfertadas(), a.getPotassioOfertado(),
                        a.getVolumePleno(), a.getProteinaNoVolumePleno(),
                        a.getProteinaSuplementar(),
                        a.getModoInfusao() == null ? null : a.getModoInfusao().getRotuloVolume(),
                        List.of(), TABELA_NAO_GRAVADA),

                new ResultadoUti.Hidratacao(
                        a.getHidratacaoNecessidadeMinima(), a.getHidratacaoNecessidadeIdeal(),
                        a.getHidratacaoPercentualAgua(), a.getHidratacaoPercentualAguaOrigem(),
                        a.getVolumeTotalMl() != null
                                ? a.getVolumeTotalMl() : a.getVolumeDietaManualMl(),
                        a.getHidratacaoAguaNaDieta(),
                        a.getHidratacaoAguaExtraMinima(), a.getHidratacaoAguaExtraIdeal(),
                        List.of(), List.of(), TABELA_NAO_GRAVADA));
    }

    /**
     * Rótulo e tom vieram gravados juntos, e voltam juntos.
     *
     * <p>Tom desconhecido cai em {@code NEUTRO} em vez de estourar: um registro
     * antigo com um tom que o enum não conhece mais deve continuar legível — o
     * rótulo é o que importa, e ele está lá.
     */
    private static Classificacao classificacao(String rotulo, String tom) {
        if (rotulo == null) return null;

        TomResultado resolvido;
        try {
            resolvido = tom == null ? TomResultado.NEUTRO : TomResultado.valueOf(tom);
        } catch (IllegalArgumentException e) {
            resolvido = TomResultado.NEUTRO;
        }

        return new Classificacao(rotulo, resolvido);
    }
}
