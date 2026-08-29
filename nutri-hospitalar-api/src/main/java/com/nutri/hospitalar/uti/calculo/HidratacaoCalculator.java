package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;
import com.nutri.hospitalar.uti.calculo.cascata.VolumeDieta;
import com.nutri.hospitalar.uti.enums.OrigemValor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CEM;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.porQuilo;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * Necessidade hídrica e quanto de água extra ofertar além da dieta.
 * Especificação: {@code docs/10} §5.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco.
 */
public final class HidratacaoCalculator {

    private HidratacaoCalculator() {}

    /** {@code Hidratação!D4}: "25 a 30 ml/Kg/dia". */
    private static final BigDecimal ML_POR_KG_MINIMO = new BigDecimal("25");
    private static final BigDecimal ML_POR_KG_IDEAL  = new BigDecimal("30");

    /** Fracionamentos oferecidos ({@code Hidratação!F16:I16}). */
    public static final List<Integer> FRACIONAMENTOS = List.of(4, 5, 6, 8);

    /**
     * A escada de % de água por densidade ({@code Hidratação!G5:H8}).
     *
     * <p><b>É fallback, não a fonte preferida.</b> O teor de água livre é
     * propriedade do produto e vem no rótulo — não se deriva da densidade. A
     * própria literatura mede 1,0 → 84 % · 1,2 → 82 % · 1,5 → 76 % · 2,0 → 70 %,
     * que não coincide com os números redondos da planilha justamente porque ela
     * aproxima por faixa.
     *
     * <p>Além disso a tabela só tem quatro densidades exatas. Das <b>53</b>
     * fórmulas do catálogo, 44 caem numa delas (1,0 · 1,2 · 1,5 · 2,0) e as
     * outras <b>9 têm densidade intermediária</b> — oito valores distintos:
     * 1,12 · 1,14 · 1,21 · 1,23 · 1,24 (duas fórmulas) · 1,3 · 1,31 · 1,33.
     * Nenhuma delas tem linha aqui, e é por isso que a ausência é devolvida com
     * motivo em vez de um degrau escolhido por aproximação.
     * Ver {@code docs/10} §5.1.
     */
    private static final Map<String, String> PERC_AGUA_POR_DENSIDADE = Map.of(
            "1.0", "85",
            "1.2", "80",
            "1.5", "75",
            "2.0", "70");

    // ─── Necessidade ────────────────────────────────────────────────────

    /** {@code peso × 25} ml/dia. Confere, peso 72: 1800 ml. */
    public static BigDecimal necessidadeMinima(PesoDeTrabalho peso) {
        return porQuilo(ML_POR_KG_MINIMO, peso);
    }

    /** {@code peso × 30} ml/dia. Confere, peso 72: 2160 ml. */
    public static BigDecimal necessidadeIdeal(PesoDeTrabalho peso) {
        return porQuilo(ML_POR_KG_IDEAL, peso);
    }

    // ─── A água que já vem na dieta ─────────────────────────────────────

    /**
     * O teor de água livre da fórmula, <b>com a sua procedência</b>.
     *
     * <p>Preferimos o dado do rótulo, guardado em
     * {@code formula_enteral.agua_livre_perc}. Faltando ele, caímos na escada
     * por densidade — e <b>dizemos que caímos</b>, em vez de fingir precisão que
     * não temos. Densidade sem linha na tabela devolve ausência, não um degrau
     * escolhido por aproximação: inventar a escada seria inferência nossa sobre
     * prescrição de UTI.
     */
    public static PercentualAgua percentualDeAgua(BigDecimal aguaLivreDoRotulo,
                                                  BigDecimal densidadeKcalMl) {
        if (aguaLivreDoRotulo != null)
            return new PercentualAgua(aguaLivreDoRotulo, OrigemValor.AGUA_LIVRE_ROTULO);

        if (densidadeKcalMl == null) return null;

        String chave = densidadeKcalMl.stripTrailingZeros().toPlainString();
        String perc = PERC_AGUA_POR_DENSIDADE.get(normalizarChave(chave));

        return perc == null
                ? null
                : new PercentualAgua(new BigDecimal(perc), OrigemValor.AGUA_LIVRE_ESTIMADA);
    }

    /**
     * {@code volume_dieta × % / 100}.
     *
     * <p>Confere, dieta 1600 ml: 1120 · 1200 · 1280 · 1360 ml para 70, 75, 80 e
     * 85 %.
     */
    public static BigDecimal aguaNaDieta(VolumeDieta volume, PercentualAgua percentual) {
        if (volume == null || percentual == null) return null;
        return volume.ml().multiply(percentual.valor(), CONTA).divide(CEM, CONTA);
    }

    // ─── A água que falta ───────────────────────────────────────────────

    /**
     * {@code necessidade − água da dieta}.
     *
     * <p>Confere, peso 72 e dieta 1600 ml: extra ideal 1040 · 960 · 880 · 800.
     *
     * <p>Nunca negativo: dieta que já entrega mais água que a necessidade
     * devolve zero, e não "ofertar −200 ml".
     *
     * <p>Na planilha, {@code Hidratação!D21} e {@code E21} auto-referenciam uma
     * célula vazia da própria aba quando deveriam ler o volume da dieta
     * artesanal — então a água extra não desconta nada (defeito 9). Aqui o
     * volume chega como {@link VolumeDieta}, tipo que só a dieta produz.
     */
    public static BigDecimal aguaExtra(BigDecimal necessidade, BigDecimal aguaNaDieta) {
        if (necessidade == null) return null;
        BigDecimal jaOfertada = aguaNaDieta == null ? BigDecimal.ZERO : aguaNaDieta;
        BigDecimal extra = necessidade.subtract(jaOfertada, CONTA);
        return extra.signum() > 0 ? extra : BigDecimal.ZERO;
    }

    /**
     * A água extra dividida no número de vezes ao dia.
     *
     * <p>A nota {@code E15} da planilha: "dividir este volume mínimo ou ideal no
     * nº de vezes que deseja ofertar". Confere, extra ideal 1040 em 6×:
     * 173,3333 ml.
     */
    public static List<FracaoAgua> distribuir(BigDecimal aguaExtra) {
        if (!positivo(aguaExtra)) return List.of();

        return FRACIONAMENTOS.stream()
                .map(n -> new FracaoAgua(n,
                        aguaExtra.divide(new BigDecimal(n), CONTA)))
                .toList();
    }

    /**
     * @param valor  % do volume da dieta que é água
     * @param origem do rótulo, ou estimada pela densidade — a tela escreve qual
     */
    public record PercentualAgua(BigDecimal valor, OrigemValor origem) {

        public String descricaoOrigem() {
            return origem.getDescricao();
        }
    }

    /**
     * @param vezesAoDia em quantas tomadas
     * @param mlPorVez   volume de cada uma
     */
    public record FracaoAgua(int vezesAoDia, BigDecimal mlPorVez) {}

    // ─────────────────────────────────────────────────────────────────────

    /**
     * {@code stripTrailingZeros} transforma 1.0 em "1" e 2.0 em "2"; a tabela
     * está escrita com uma casa. Normalizar aqui evita quatro chaves duplicadas
     * no mapa.
     */
    private static String normalizarChave(String valor) {
        return valor.contains(".") ? valor : valor + ".0";
    }
}
