package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.uti.calculo.cascata.MetaEnergetica;
import com.nutri.hospitalar.uti.calculo.cascata.MetaProteica;
import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;
import com.nutri.hospitalar.uti.calculo.cascata.VolumeDieta;
import com.nutri.hospitalar.uti.enums.ModoInfusao;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.MIL;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.dividir;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.percentual;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * A dieta enteral industrializada: o que a prescrição atual entrega e o que
 * faltaria para bater a meta. Especificação: {@code docs/10} §4.
 *
 * <p><b>Uma classe, não duas.</b> A planilha tem duas abas quase idênticas —
 * {@code Cálculos dietas_Contínuo} com 52 linhas e {@code _Intermitente} com 50
 * — e cinco dos 22 defeitos vieram dessa duplicação: uma aba divide a proteína
 * por 1000 e a outra por 500 para o mesmo produto (defeito 1), uma divide
 * kcal/kg pela altura (defeito 4) e a outra pelo VCT (defeito 5). O modo só
 * muda o rótulo; a matemática é a mesma, e há teste que prova isso.
 *
 * <p><b>Toda composição chega por litro.</b> O catálogo é normalizado na
 * migration 018 e tem {@code CHECK} de fechamento energético — não existe aqui
 * ramo de "produto em frasco de 500 ml".
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco.
 */
public final class DietaEnteralCalculator {

    private DietaEnteralCalculator() {}

    /** Os degraus de progressão dos dias 1 a 4 ({@code Contínuo!P15:P18}). */
    private static final List<BigDecimal> DEGRAUS_PROGRESSAO = List.of(
            new BigDecimal("25"), new BigDecimal("50"),
            new BigDecimal("75"), new BigDecimal("100"));

    /** A nota {@code V21} da planilha: "iniciar o módulo a partir do 4º dia". */
    public static final int DIA_INICIO_MODULO = 4;

    // ─── O que a prescrição entrega ─────────────────────────────────────

    /**
     * Volume total do dia: {@code volume × tempo}.
     *
     * <p>{@code tempo} é horas de infusão no contínuo e número de horários no
     * intermitente — a mesma multiplicação nos dois. Confere: 62 ml/h × 22 h =
     * 1364 ml; 133 ml × 6 horários = 798 ml.
     */
    public static VolumeDieta volumeTotal(BigDecimal volumePorTempo, BigDecimal tempo,
                                          ModoInfusao modo) {
        if (!positivo(volumePorTempo) || !positivo(tempo) || modo == null) return null;

        BigDecimal vt = volumePorTempo.multiply(tempo, CONTA);
        String descricao = "%s %s × %s %s".formatted(
                volumePorTempo.stripTrailingZeros().toPlainString(), modo.getRotuloVolume(),
                tempo.stripTrailingZeros().toPlainString(), modo.getRotuloTempo());

        return new VolumeDieta(vt, descricao);
    }

    /** {@code densidade × VT}. Confere: 1,0 kcal/ml × 1364 ml = 1364 kcal. */
    public static BigDecimal caloriasOfertadas(BigDecimal densidadeKcalMl, VolumeDieta volume) {
        if (densidadeKcalMl == null || volume == null) return null;
        return densidadeKcalMl.multiply(volume.ml(), CONTA);
    }

    /**
     * {@code ptn_por_litro × VT / 1000}.
     *
     * <p>Divide por 1000 <b>sempre</b>, porque o catálogo é sempre por litro.
     * É aqui que morava o pior defeito da planilha: a aba Contínuo dividia por
     * 1000 a composição de um produto declarado por frasco de 500 ml e
     * subestimava a proteína em 50 %.
     *
     * <p>Confere: 92 g/L × 1364 ml / 1000 = 125,488 g.
     */
    public static BigDecimal proteinaOfertada(BigDecimal proteinaGL, VolumeDieta volume) {
        if (proteinaGL == null || volume == null) return null;
        return proteinaGL.multiply(volume.ml(), CONTA).divide(MIL, CONTA);
    }

    /** Confere: 1364 kcal / 68 kg = 20,0588 kcal/kg. */
    public static BigDecimal caloriasPorQuilo(BigDecimal caloriasOfertadas, PesoDeTrabalho peso) {
        if (peso == null) return null;
        return dividir(caloriasOfertadas, peso.valorKg());
    }

    /** Confere: 125,488 g / 68 kg = 1,8454 g/kg. */
    public static BigDecimal proteinaPorQuilo(BigDecimal proteinaOfertada, PesoDeTrabalho peso) {
        if (peso == null) return null;
        return dividir(proteinaOfertada, peso.valorKg());
    }

    // ─── Quanto da meta a prescrição cobre ──────────────────────────────

    /**
     * {@code kcal ofertadas × 100 / meta}.
     *
     * <p>Divide pela <b>meta energética</b>, e é preciso dizer isso porque 11
     * das 52 linhas da planilha dividem pela proteína total (defeito 3), o que
     * devolve percentual absurdo. A meta chega como {@link MetaEnergetica} —
     * tipo distinto de {@link MetaProteica} — para que a troca não compile.
     *
     * <p>Confere: 1364 / 1360 × 100 = 100,2941 %.
     */
    public static BigDecimal percentualDoVct(BigDecimal caloriasOfertadas, MetaEnergetica meta) {
        if (meta == null) return null;
        return percentual(caloriasOfertadas, meta.kcalDia());
    }

    /** Confere: 125,488 / 102 × 100 = 123,0275 %. */
    public static BigDecimal percentualDaProteina(BigDecimal proteinaOfertada, MetaProteica meta) {
        if (meta == null) return null;
        return percentual(proteinaOfertada, meta.gramasDia());
    }

    // ─── O que seria preciso para bater a meta ──────────────────────────

    /**
     * Volume pleno: {@code (meta / densidade) / tempo}, na unidade do modo.
     *
     * <p>É a vazão que entregaria a meta calórica inteira. Confere: 1360 kcal /
     * 1,0 kcal/ml / 22 h = 61,8182 ml/h; 1800 / 1,24 / 6 = 241,9355 ml/horário.
     */
    public static BigDecimal volumePleno(MetaEnergetica meta, BigDecimal densidadeKcalMl,
                                         BigDecimal tempo) {
        if (meta == null || !positivo(densidadeKcalMl) || !positivo(tempo)) return null;
        return meta.kcalDia().divide(densidadeKcalMl, CONTA).divide(tempo, CONTA);
    }

    /**
     * A proteína que o volume pleno entregaria:
     * {@code volume_pleno × tempo × ptn_por_litro / 1000}.
     *
     * <p>Confere: 61,8182 × 22 × 92 / 1000 = 125,12 g.
     */
    public static BigDecimal proteinaNoVolumePleno(BigDecimal volumePleno, BigDecimal tempo,
                                                   BigDecimal proteinaGL) {
        if (volumePleno == null || tempo == null || proteinaGL == null) return null;
        return volumePleno.multiply(tempo, CONTA)
                .multiply(proteinaGL, CONTA)
                .divide(MIL, CONTA);
    }

    /**
     * A lacuna proteica: {@code meta − ofertado}.
     *
     * <p>Zero ou negativo significa meta atingida — devolve zero, não número
     * negativo, porque "faltam −8 g" não é frase que se ponha na frente de quem
     * prescreve. Na planilha, {@code Intermitente!N11} usa uma célula vazia como
     * meta e devolve a proteína inteira como lacuna (defeito 8).
     */
    public static BigDecimal proteinaSuplementar(MetaProteica meta, BigDecimal proteinaOfertada) {
        if (meta == null || proteinaOfertada == null) return null;
        BigDecimal lacuna = meta.gramasDia().subtract(proteinaOfertada, CONTA);
        return lacuna.signum() > 0 ? lacuna : BigDecimal.ZERO;
    }

    // ─── Progressão ─────────────────────────────────────────────────────

    /**
     * A tabela de progressão dos dias 1 a 4 — 25, 50, 75 e 100 % da meta.
     *
     * <p>Confere, meta 1360 e 22 h a 1,5 kcal/ml: 10,3030 · 20,6061 · 30,9091 ·
     * 41,2121 ml/h. E meta 1800 em 6 horários a 1,5: 50 · 100 · 150 · 200.
     */
    public static List<DegrauProgressao> progressao(MetaEnergetica meta,
                                                    BigDecimal densidadeKcalMl,
                                                    BigDecimal tempo) {
        if (meta == null || !positivo(densidadeKcalMl) || !positivo(tempo)) return List.of();

        return IntStream.range(0, DEGRAUS_PROGRESSAO.size())
                .mapToObj(i -> {
                    BigDecimal pct = DEGRAUS_PROGRESSAO.get(i);
                    BigDecimal kcalDoDia = meta.kcalDia()
                            .multiply(pct, CONTA).divide(UtiMatematica.CEM, CONTA);
                    BigDecimal volume = kcalDoDia.divide(densidadeKcalMl, CONTA)
                            .divide(tempo, CONTA);
                    return new DegrauProgressao(i + 1, pct, kcalDoDia, volume);
                })
                .toList();
    }

    /**
     * @param dia    1 a 4
     * @param pct    percentual da meta naquele dia
     * @param kcal   kcal/dia do degrau
     * @param volume ml/h ou ml/horário, conforme o modo
     */
    public record DegrauProgressao(int dia, BigDecimal pct, BigDecimal kcal, BigDecimal volume) {}

    // ─── Módulo proteico ────────────────────────────────────────────────

    /**
     * Quanto de um módulo proteico cobre a lacuna, e quantas calorias isso soma.
     *
     * <pre>
     * gramas_de_produto = lacuna × medida_g / ptn_por_medida
     * kcal_adicionadas  = gramas_de_produto / medida_g × kcal_por_medida
     * </pre>
     *
     * <p>Confere, lacuna de 45 g com Nutren Just Protein (medida 15 g, 13 g PTN,
     * 52 kcal): 51,9231 g = 3,4615 medidas × 52 = 180,0 kcal.
     *
     * <p><b>A kcal sai da composição do produto, nunca recomposta macro a
     * macro</b> — e é essa escolha que evita o defeito 10. A planilha faz
     * {@code lacuna×4 + gramas×9,66/20} em {@code Contínuo!U24}, somando
     * <b>gramas</b> de carboidrato a um total de <b>kcal</b>: para o Nutridrink
     * Protein ela devolve 252,45 onde o correto pela composição é 469,8. Os dois
     * caminhos concordam nos módulos sem carboidrato e discordam no único com
     * — a prova aritmética do erro.
     *
     * @param lacunaG        proteína que falta, em g/dia
     * @param medidaG        gramas de uma medida do produto
     * @param proteinaPorMedidaG proteína em uma medida
     * @param kcalPorMedida  calorias em uma medida, como o rótulo declara
     */
    public static SugestaoModulo moduloProteico(BigDecimal lacunaG, BigDecimal medidaG,
                                                BigDecimal proteinaPorMedidaG,
                                                BigDecimal kcalPorMedida) {
        if (!positivo(lacunaG) || !positivo(medidaG)
                || !positivo(proteinaPorMedidaG) || kcalPorMedida == null) return null;

        BigDecimal gramas = lacunaG.multiply(medidaG, CONTA).divide(proteinaPorMedidaG, CONTA);
        BigDecimal medidas = gramas.divide(medidaG, CONTA);
        BigDecimal kcal = medidas.multiply(kcalPorMedida, CONTA);

        return new SugestaoModulo(gramas, medidas, kcal);
    }

    /**
     * @param gramas  gramas de produto por dia
     * @param medidas o mesmo em número de medidas, que é como se prescreve
     * @param kcal    calorias que o módulo soma ao dia
     */
    public record SugestaoModulo(BigDecimal gramas, BigDecimal medidas, BigDecimal kcal) {}
}
