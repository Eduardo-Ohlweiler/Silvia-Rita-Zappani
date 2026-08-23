package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * As primitivas de que todos os calculadores da UTI dependem.
 *
 * <p>Existem porque três operações aparecem em cerca de 60 das 123 células de
 * fórmula da planilha: <b>algo por quilo</b>, <b>percentual de algo</b> e
 * <b>gramas viram calorias</b>. Escrevê-las uma vez é o que impede que a
 * quinquagésima repetição saia diferente — que é exatamente o que aconteceu na
 * planilha, onde 11 das 52 linhas dividem o %VCT pela proteína em vez de pelo
 * VCT (defeito 3 de {@code docs/10} §11).
 *
 * <p>Contas intermediárias correm em {@link MathContext#DECIMAL64} — 16 dígitos
 * significativos, a mesma ordem do LibreOffice — e o arredondamento acontece
 * <b>uma vez</b>, na saída.
 */
public final class UtiMatematica {

    private UtiMatematica() {}

    /** Precisão das contas intermediárias, antes do arredondamento final. */
    public static final MathContext CONTA = MathContext.DECIMAL64;

    /** Escala dos resultados — casa com NUMERIC(12,4). */
    public static final int ESCALA = 4;

    /** Escala dos percentuais — casa com NUMERIC(6,2). */
    public static final int ESCALA_PERCENTUAL = 2;

    public static final BigDecimal CEM = new BigDecimal("100");
    public static final BigDecimal MIL = new BigDecimal("1000");

    /** Fatores de Atwater. Só {@link #kcalDeMacros} os usa — ver o javadoc de lá. */
    private static final BigDecimal KCAL_POR_G_CHO = new BigDecimal("4");
    private static final BigDecimal KCAL_POR_G_PTN = new BigDecimal("4");
    private static final BigDecimal KCAL_POR_G_LIP = new BigDecimal("9");

    // ─── As três primitivas ─────────────────────────────────────────────

    /**
     * Um alvo por quilo aplicado ao peso de trabalho.
     *
     * <p>Recebe {@link PesoDeTrabalho} e não um {@code BigDecimal} solto: é a
     * assinatura que impede uma etapa de usar um peso diferente da anterior.
     * Ver {@code docs/10} §11, defeitos 14 e 15.
     */
    public static BigDecimal porQuilo(BigDecimal valorPorKg, PesoDeTrabalho peso) {
        if (valorPorKg == null || peso == null) return null;
        return valorPorKg.multiply(peso.valorKg(), CONTA);
    }

    /**
     * {@code parte / todo × 100}.
     *
     * <p>Devolve {@code null} quando o todo é nulo ou zero — nunca infinito,
     * nunca {@code #DIV/0!} gravado em célula, que é o que a planilha faz 446
     * vezes na aba {@code Prescr x Inf}.
     */
    public static BigDecimal percentual(BigDecimal parte, BigDecimal todo) {
        if (parte == null || todo == null || todo.signum() == 0) return null;
        return parte.divide(todo, CONTA).multiply(CEM, CONTA);
    }

    /**
     * <b>A única porta pela qual um grama vira caloria neste sistema.</b>
     *
     * <p>{@code CHO×4 + PTN×4 + LIP×9}, Atwater. Concentrar a conversão aqui é o
     * que torna o defeito 10 da planilha inexprimível: lá, {@code Contínuo!U24}
     * soma <b>gramas</b> de carboidrato a um total de <b>kcal</b> e devolve
     * 252,45 onde o certo é 469,8. Não existindo outro ponto onde um
     * {@code BigDecimal} em gramas encontre um em kcal, o erro não tem onde
     * acontecer.
     *
     * <p>A própria planilha valida a primitiva contra si mesma: na dieta
     * artesanal, {@code 261,1517×4 + 92,4505×4 + 59,1379×9 = 1946,6498},
     * idêntico ao total em cache ({@code docs/10} §7).
     *
     * <p>Macro ausente conta como zero — aqui, e só aqui, isso é correto: não
     * declarar lipídio não significa lipídio desconhecido, significa que ele não
     * entra na soma pedida.
     */
    public static BigDecimal kcalDeMacros(BigDecimal choG, BigDecimal ptnG, BigDecimal lipG) {
        return zeroSeNulo(choG).multiply(KCAL_POR_G_CHO, CONTA)
                .add(zeroSeNulo(ptnG).multiply(KCAL_POR_G_PTN, CONTA), CONTA)
                .add(zeroSeNulo(lipG).multiply(KCAL_POR_G_LIP, CONTA), CONTA);
    }

    // ─── Utilitários ────────────────────────────────────────────────────

    /** Divisão que devolve ausência em vez de estourar. */
    public static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        if (dividendo == null || divisor == null || divisor.signum() == 0) return null;
        return dividendo.divide(divisor, CONTA);
    }

    public static BigDecimal multiplicar(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.multiply(b, CONTA);
    }

    /** Arredonda uma vez, na saída, com a escala declarada. */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public static BigDecimal arredondarPercentual(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    /** Maior que zero. Nulo e zero são a mesma coisa para quem vai dividir. */
    public static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
