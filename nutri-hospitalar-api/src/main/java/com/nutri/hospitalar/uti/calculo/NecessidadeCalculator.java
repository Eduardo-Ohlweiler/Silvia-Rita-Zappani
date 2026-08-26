package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.uti.calculo.cascata.MetaEnergetica;
import com.nutri.hospitalar.uti.calculo.cascata.MetaProteica;
import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;
import com.nutri.hospitalar.uti.enums.FaseTerapia;
import com.nutri.hospitalar.uti.enums.OrigemValor;
import com.nutri.hospitalar.uti.enums.PosicaoNaFaixa;
import com.nutri.hospitalar.uti.enums.TerapiaRenal;

import java.math.BigDecimal;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.porQuilo;

/**
 * Necessidades energéticas e proteicas. Especificação: {@code docs/10} §3.
 *
 * <p><b>Tudo é kcal/kg e g/kg.</b> Não há equação preditiva de gasto energético
 * na planilha — nem Harris-Benedict, nem Mifflin-St Jeor, nem Ireton-Jones. Se
 * um dia entrar uma, entra como fórmula nova e documentada, não por inferência.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco.
 */
public final class NecessidadeCalculator {

    private NecessidadeCalculator() {}

    // Fase aguda — Necessidades!B4/B5 e B7/B8
    private static final BigDecimal AGUDA_KCAL_MIN = new BigDecimal("15");
    private static final BigDecimal AGUDA_KCAL_MAX = new BigDecimal("20");
    private static final BigDecimal AGUDA_PTN_MIN  = new BigDecimal("1.2");
    private static final BigDecimal AGUDA_PTN_MAX  = new BigDecimal("1.5");

    // Reabilitação — Necessidades!C4/C5 e C7/C8
    private static final BigDecimal REAB_KCAL_MIN  = new BigDecimal("25");
    private static final BigDecimal REAB_KCAL_MAX  = new BigDecimal("30");
    private static final BigDecimal REAB_PTN_MIN   = new BigDecimal("1.5");
    private static final BigDecimal REAB_PTN_MAX   = new BigDecimal("2.0");

    // Obesidade — Necessidades!B15:C17
    private static final BigDecimal OBESO_KCAL_MIN     = new BigDecimal("11");
    private static final BigDecimal OBESO_KCAL_MAX     = new BigDecimal("14");
    private static final BigDecimal OBESO_G3_KCAL_MIN  = new BigDecimal("22");
    private static final BigDecimal OBESO_G3_KCAL_MAX  = new BigDecimal("25");
    private static final BigDecimal OBESO_PTN          = new BigDecimal("2.0");
    private static final BigDecimal OBESO_GRAVE_PTN    = new BigDecimal("2.5");

    /** A partir daqui vale o protocolo de obesidade, e a fase não se aplica. */
    public static final BigDecimal IMC_OBESIDADE = new BigDecimal("30");

    /**
     * A partir daqui a proteína sobe para 2,5 g/kg — é o corte do cabeçalho
     * {@code IMC>40} ({@code Necessidades!C13}).
     *
     * <p>ASPEN/SCCM 2016, recomendação C4: 2,0 g/kg de peso ideal em IMC 30–40 e
     * <b>2,5 g/kg em IMC ≥ 40</b>.
     * (<a href="https://www.guidelinecentral.com/guideline/39804/">resumo</a> ·
     * <a href="https://nutritioncare.org/text-based-resource/feeding-the-patient-with-obesity-in-the-critical-care-setting-discussion-and-key-recommendations/">ASPEN</a>)
     */
    public static final BigDecimal IMC_PROTEINA_MAXIMA = new BigDecimal("40");

    /**
     * A partir daqui a energia passa a ser calculada sobre o peso ideal — é o
     * corte do rodapé {@code IMC >50} ({@code Necessidades!C18}).
     *
     * <p>ASPEN/SCCM 2016, recomendação C3: 11–14 kcal/kg de <b>peso atual</b> em
     * IMC 30–50 e 22–25 kcal/kg de <b>peso ideal</b> em IMC > 50.
     *
     * <p><b>Os dois rótulos da planilha não se contradizem</b> — cabeçalho e
     * rodapé pertencem a <i>linhas</i> diferentes da mesma coluna: {@code IMC>40}
     * é o corte da proteína, {@code IMC >50} é o da energia. Ver {@code docs/10}
     * §3.3 e o defeito 18 de §11.
     */
    public static final BigDecimal IMC_OBESIDADE_GRAVE = new BigDecimal("50");

    // ─── Faixa por fase ─────────────────────────────────────────────────

    /**
     * A faixa energética da fase, em kcal/dia.
     *
     * <p>Confere, peso 68: aguda 1020–1360, reabilitação 1700–2040.
     */
    public static Faixa energiaPorFase(FaseTerapia fase, PesoDeTrabalho peso) {
        if (fase == null || peso == null) return null;
        return fase == FaseTerapia.AGUDA
                ? new Faixa(porQuilo(AGUDA_KCAL_MIN, peso), porQuilo(AGUDA_KCAL_MAX, peso))
                : new Faixa(porQuilo(REAB_KCAL_MIN, peso),  porQuilo(REAB_KCAL_MAX, peso));
    }

    /**
     * A faixa proteica da fase, em g/dia.
     *
     * <p>Confere, peso 68: aguda 81,6–102, reabilitação 102–136. O rótulo da
     * planilha em reabilitação é {@code >1,5} ({@code C6}), mas as fórmulas
     * calculam 1,5 e 2,0 — a faixa implementada é a das fórmulas.
     */
    public static Faixa proteinaPorFase(FaseTerapia fase, PesoDeTrabalho peso) {
        if (fase == null || peso == null) return null;
        return fase == FaseTerapia.AGUDA
                ? new Faixa(porQuilo(AGUDA_PTN_MIN, peso), porQuilo(AGUDA_PTN_MAX, peso))
                : new Faixa(porQuilo(REAB_PTN_MIN, peso),  porQuilo(REAB_PTN_MAX, peso));
    }

    /**
     * Proteína na terapia renal substitutiva, que <b>substitui</b> a faixa da
     * fase ({@code Necessidades!A9:B10}).
     *
     * <p>Confere, peso 68: intermitente 122,4 g, contínua 136 g.
     */
    public static BigDecimal proteinaTerapiaRenal(TerapiaRenal terapia, PesoDeTrabalho peso) {
        if (terapia == null || terapia == TerapiaRenal.NENHUMA || peso == null) return null;
        return porQuilo(terapia.getProteinaGKg(), peso);
    }

    // ─── Obesidade ──────────────────────────────────────────────────────

    /**
     * A faixa energética no obeso.
     *
     * <p><b>A base do peso muda com o IMC, e é o erro mais fácil de cometer.</b>
     * Até IMC 50 a energia é 11–14 kcal/kg de <b>peso atual</b>; acima de 50 é
     * 22–25 kcal/kg de <b>peso ideal</b>. Na planilha isso são duas células
     * separadas ({@code F2} e {@code F3}) que o usuário confunde.
     *
     * <p>Por isso este método recebe os <b>dois</b> pesos e escolhe internamente:
     * o chamador não pode ter essa liberdade.
     *
     * <p>Confere, peso 68 e ideal 82: 748–952 (IMC 30–50) e 1804–2050 (IMC ≥ 50).
     */
    public static Faixa energiaObesidade(BigDecimal imc, PesoDeTrabalho pesoAtual,
                                         PesoDeTrabalho pesoIdeal) {
        if (imc == null) return null;

        if (imc.compareTo(IMC_OBESIDADE_GRAVE) >= 0) {
            if (pesoIdeal == null) return null;
            return new Faixa(porQuilo(OBESO_G3_KCAL_MIN, pesoIdeal),
                             porQuilo(OBESO_G3_KCAL_MAX, pesoIdeal));
        }

        if (pesoAtual == null) return null;
        return new Faixa(porQuilo(OBESO_KCAL_MIN, pesoAtual),
                         porQuilo(OBESO_KCAL_MAX, pesoAtual));
    }

    /**
     * Proteína no obeso — <b>sempre sobre o peso ideal</b>, nas duas faixas.
     *
     * <p>2,0 g/kg até IMC 40 e 2,5 g/kg daí em diante. Confere, ideal 82:
     * 164 g e 205 g.
     */
    public static BigDecimal proteinaObesidade(BigDecimal imc, PesoDeTrabalho pesoIdeal) {
        if (imc == null || pesoIdeal == null) return null;
        BigDecimal gKg = imc.compareTo(IMC_PROTEINA_MAXIMA) >= 0 ? OBESO_GRAVE_PTN : OBESO_PTN;
        return porQuilo(gKg, pesoIdeal);
    }

    /** O protocolo de obesidade se aplica, e a fase da terapia não. */
    public static boolean ehObeso(BigDecimal imc) {
        return imc != null && imc.compareTo(IMC_OBESIDADE) >= 0;
    }

    // ─── Personalizado ──────────────────────────────────────────────────

    /**
     * Alvo digitado pelo profissional ({@code Necessidades!F5:G9}).
     *
     * <p>Confere: 35 kcal/kg e 1,3 g/kg com peso 68 → 2380 kcal e 88,4 g.
     */
    public static MetaEnergetica energiaPersonalizada(BigDecimal kcalPorKg, PesoDeTrabalho peso) {
        BigDecimal total = porQuilo(kcalPorKg, peso);
        return total == null ? null : new MetaEnergetica(total, OrigemValor.META_PERSONALIZADA);
    }

    public static MetaProteica proteinaPersonalizada(BigDecimal gPorKg, PesoDeTrabalho peso) {
        BigDecimal total = porQuilo(gPorKg, peso);
        return total == null ? null : new MetaProteica(total, OrigemValor.META_PERSONALIZADA);
    }

    // ─── A meta que a dieta vai perseguir ───────────────────────────────

    /**
     * Um ponto da faixa vira a meta que desce para a dieta enteral.
     *
     * <p>Escolher <b>qual</b> ponto é decisão de quem prescreve, e ela deixou de
     * ser silenciosa: a planilha não escolhe — tabula a faixa e deixa a meta ser
     * <b>redigitada à mão</b> na aba da dieta (defeito 14) — e o eroERP escolhe
     * o ponto médio sem dizer. Aqui a posição é entrada, o padrão é o topo (o
     * que o exemplo em cache da planilha mostra) e a meta carrega a escolha
     * junto com a origem. Ver {@link PosicaoNaFaixa}.
     */
    public static MetaEnergetica metaDaFaixa(Faixa faixa, OrigemValor origem,
                                             PosicaoNaFaixa posicao) {
        BigDecimal valor = pontoDaFaixa(faixa, posicao);
        return valor == null ? null : new MetaEnergetica(valor, origem, posicaoOuPadrao(posicao));
    }

    public static MetaProteica metaProteicaDaFaixa(Faixa faixa, OrigemValor origem,
                                                   PosicaoNaFaixa posicao) {
        BigDecimal valor = pontoDaFaixa(faixa, posicao);
        return valor == null ? null : new MetaProteica(valor, origem, posicaoOuPadrao(posicao));
    }

    /** O topo é o padrão, e é o que a planilha mostra no seu próprio exemplo. */
    public static PosicaoNaFaixa posicaoOuPadrao(PosicaoNaFaixa posicao) {
        return posicao == null ? PosicaoNaFaixa.MAXIMO : posicao;
    }

    private static BigDecimal pontoDaFaixa(Faixa faixa, PosicaoNaFaixa posicao) {
        if (faixa == null) return null;
        return switch (posicaoOuPadrao(posicao)) {
            case MINIMO -> faixa.minimo();
            case MEDIO  -> faixa.medio();
            case MAXIMO -> faixa.maximo();
        };
    }

    /**
     * Uma faixa de recomendação — mínimo e máximo na mesma unidade.
     *
     * <p>Existe como tipo porque a planilha trata mínimo e máximo como células
     * soltas, e é assim que três linhas acabam dividindo kcal/kg pela altura em
     * vez do peso (defeitos 4 e 5).
     */
    public record Faixa(BigDecimal minimo, BigDecimal maximo) {

        /** O ponto médio, quando a tela precisa de um número só. */
        public BigDecimal medio() {
            if (minimo == null || maximo == null) return null;
            return minimo.add(maximo, CONTA).divide(new BigDecimal("2"), CONTA);
        }
    }
}
