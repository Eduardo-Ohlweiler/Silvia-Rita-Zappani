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
 * <p>O ponto delicado é o {@code resultado}: <b>todo número é reconstruído das
 * colunas gravadas</b>, nunca recalculado. Um mapper que tirasse número do
 * calculador faria exatamente o que o registro clínico não pode fazer — mudar
 * de valor quando a régua muda.
 *
 * <p><b>Os motivos são a exceção, e não são número.</b> Eles explicam por que
 * uma coluna está vazia, e isso é função das <b>entradas</b>, que estão
 * gravadas na própria avaliação. Antes vinham todos nulos, e uma avaliação sem
 * circunferência do braço reabria com o bloco em branco e sem uma palavra — o
 * traço mudo que {@link ResultadoUti} existe para não produzir. Hoje
 * {@link #toResultado(AvaliacaoUti, ResultadoUti)} recebe um cálculo feito
 * sobre as entradas gravadas e colhe dele <b>apenas as frases</b>.
 *
 * <p>Duas coisas ficam de fora da reconstrução, e é deliberado:
 * a <b>progressão da dieta</b> e a <b>distribuição da água</b> são tabelas
 * derivadas de valores que já estão gravados, e guardá-las linha a linha seria
 * duplicar dado sem ganho. Elas voltam vazias com {@link #TABELA_NAO_GRAVADA}
 * no motivo <b>próprio de cada tabela</b> — e não no motivo do bloco, onde a
 * frase acabava colada a números que existem.
 */
public final class AvaliacaoUtiMapper {

    private AvaliacaoUtiMapper() {}

    static final String TABELA_NAO_GRAVADA =
            "Tabela derivada — não faz parte do registro. Reproduza na calculadora se precisar.";

    /**
     * @param motivos cálculo refeito sobre as entradas gravadas, do qual só as
     *                frases de ausência são aproveitadas
     */
    public static AvaliacaoUtiResponseDto toResponse(AvaliacaoUti a, ResultadoUti motivos) {
        return new AvaliacaoUtiResponseDto(
                a.getId(),
                a.getPaciente().getId(),
                a.getPaciente().getNome(),
                a.getProfissional() == null ? null : a.getProfissional().getId(),
                a.getProfissional() == null ? null : a.getProfissional().getNome(),
                a.getDataAvaliacao(),
                toEntradas(a),
                toResultado(a, motivos),
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
                a.getModuloProteico() == null ? null : a.getModuloProteico().getId(),
                a.getVolumeDietaManualMl());
    }

    /**
     * Os resultados: <b>números do banco, motivos do recálculo</b>.
     *
     * <p><b>De {@code motivos} sai apenas texto derivado das entradas</b> — as
     * frases de ausência e o rótulo de {@code volumeTotalDescricao}, que diz
     * como o volume foi obtido ("62 ml/h por 22 h") e também vinha mudo. Nunca
     * um número: se um escapar por aqui, a avaliação passa a mudar sozinha
     * quando o catálogo ou a régua mudarem, que é precisamente o que este
     * mapper existe para impedir.
     *
     * <p>{@code ajustePeloImcRelevante} é o outro caso de fronteira e vem de lá
     * de propósito: é um {@code boolean} de <b>apresentação</b> — decide se a
     * tela mostra o aviso do ajuste de CB e CP pelo IMC — e depende só das
     * entradas, que estão gravadas.
     */
    public static ResultadoUti toResultado(AvaliacaoUti a, ResultadoUti motivos) {
        ResultadoUti.Antropometria mAntro = motivos.antropometria();
        ResultadoUti.Necessidades mNec = motivos.necessidades();
        ResultadoUti.Dieta mDieta = motivos.dieta();
        ResultadoUti.Hidratacao mHidra = motivos.hidratacao();

        return new ResultadoUti(
                new ResultadoUti.Antropometria(
                        a.getAlturaEstimadaCm(), a.getPesoChumleaKg(),
                        a.getPesoJungKg(), a.getPesoRabitoKg(), mAntro.motivoEstimativas(),

                        a.getPesoTrabalhoKg(), a.getPesoTrabalhoOrigem(),
                        a.getAlturaUsadaCm(), a.getAlturaUsadaOrigem(),
                        mAntro.motivoPesoDeTrabalho(),

                        a.getImc(),
                        classificacao(a.getClassifImcOms(), a.getClassifImcOmsTom()),
                        classificacao(a.getClassifImcOpas(), a.getClassifImcOpasTom()),
                        mAntro.motivoImc(),

                        a.getPesoIdealKg(), a.getPesoIdealImc25Kg(),
                        a.getPesoAjustadoKg(), a.getPesoAmputacaoKg(),

                        a.getPercPerdaPeso(),
                        classificacao(a.getClassifPerdaPeso(), a.getClassifPerdaPesoTom()),
                        mAntro.motivoPerdaPeso(),

                        a.getP50CircBracoCm(), a.getAdequacaoCircBracoPerc(),
                        classificacao(a.getClassifAdequacaoCb(), a.getClassifAdequacaoCbTom()),
                        mAntro.motivoAdequacaoCircBraco(),

                        a.getCircBracoAjustadaCm(),
                        classificacao(a.getClassifMassaBraco(), a.getClassifMassaBracoTom()),
                        a.getCircPanturrilhaAjustadaCm(),
                        classificacao(a.getClassifDeplecaoCp(), a.getClassifDeplecaoCpTom()),
                        a.getPopulacaoReferencia() == null
                                ? null : a.getPopulacaoReferencia().getDescricao(),
                        // Se o ajuste pelo IMC era relevante depende só das
                        // entradas — e elas estão gravadas.
                        mAntro.ajustePeloImcRelevante(),
                        mAntro.motivoDeplecao()),

                new ResultadoUti.Necessidades(
                        a.getEnergiaMinima(), a.getEnergiaMaxima(),
                        a.getProteinaMinima(), a.getProteinaMaxima(),
                        a.getMetaEnergetica(), a.getMetaEnergeticaOrigem(),
                        a.getMetaProteica(), a.getMetaProteicaOrigem(),
                        a.getProteinaTerapiaRenal(),
                        Boolean.TRUE.equals(a.getObeso()), a.getBaseDoPeso(), mNec.motivo()),

                new ResultadoUti.Dieta(
                        a.getFormulaNome(), a.getFormulaDensidadeKcalMl(), a.getFormulaProteinaGL(),
                        a.getVolumeTotalMl(), mDieta.volumeTotalDescricao(),
                        a.getCaloriasOfertadas(), a.getProteinaOfertada(),
                        a.getCaloriasPorQuilo(), a.getProteinaPorQuilo(),
                        a.getPercentualDoVct(), a.getPercentualDaProteina(),
                        a.getChoOfertado(), a.getLipOfertado(),
                        a.getFibrasOfertadas(), a.getPotassioOfertado(),
                        a.getVolumePleno(), a.getProteinaNoVolumePleno(),
                        a.getProteinaSuplementar(),
                        a.getModoInfusao() == null ? null : a.getModoInfusao().getRotuloVolume(),
                        // Nome e números do módulo saem das colunas; só a frase
                        // que explica a ausência vem do recálculo — e ele roda
                        // sobre o RETRATO do módulo, não sobre o catálogo, senão
                        // explicaria uma sugestão que não é aquela.
                        a.getModuloNome(), a.getModuloGramas(),
                        a.getModuloMedidas(), a.getModuloKcal(),
                        mDieta.motivoModulo(),
                        // A tabela derivada explica a si mesma; o bloco
                        // explica o bloco. Antes era uma frase só, e ela
                        // aparecia ao lado de números que existiam.
                        List.of(), TABELA_NAO_GRAVADA, mDieta.motivo()),

                new ResultadoUti.Hidratacao(
                        a.getHidratacaoNecessidadeMinima(), a.getHidratacaoNecessidadeIdeal(),
                        a.getHidratacaoPercentualAgua(), a.getHidratacaoPercentualAguaOrigem(),
                        a.getVolumeTotalMl() != null
                                ? a.getVolumeTotalMl() : a.getVolumeDietaManualMl(),
                        a.getHidratacaoAguaNaDieta(),
                        a.getHidratacaoAguaExtraMinima(), a.getHidratacaoAguaExtraIdeal(),
                        List.of(), List.of(), TABELA_NAO_GRAVADA, mHidra.motivo()));
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
