package com.nutri.hospitalar.uti.calculo;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.MIL;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.dividir;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.percentual;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * O que se deriva de um dia de acompanhamento.
 *
 * <p>Nenhum destes valores é coluna: todos saem do que foi medido mais a
 * avaliação vinculada. Guardá-los seria criar duas versões do mesmo número — que
 * é exatamente o que o eroERP faz com o {@code % recebido}, campo digitável ao
 * lado de um calculado, os dois indo para o banco.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco.
 */
public final class AcompanhamentoCalculator {

    private AcompanhamentoCalculator() {}

    private static final BigDecimal HORAS_DO_DIA = new BigDecimal("24");

    /**
     * {@code recebido / prescrito × 100}.
     *
     * <p>O prescrito preferido é o da <b>avaliação</b>, não o digitado no dia:
     * é o que estava de fato prescrito, e não depende de alguém repetir o número
     * certo. Sem avaliação vinculada, cai no volume digitado — e a tela diz
     * contra o quê comparou.
     */
    public static BigDecimal percentualRecebido(BigDecimal volRecebido,
                                                BigDecimal volPrescritoDaAvaliacao,
                                                BigDecimal volPrescritoDigitado) {
        BigDecimal referencia = positivo(volPrescritoDaAvaliacao)
                ? volPrescritoDaAvaliacao : volPrescritoDigitado;
        return percentual(volRecebido, referencia);
    }

    /** {@code volume × densidade}. Precisa da fórmula da avaliação. */
    public static BigDecimal caloriasRecebidas(BigDecimal volRecebido, BigDecimal densidadeKcalMl) {
        return UtiMatematica.multiplicar(volRecebido, densidadeKcalMl);
    }

    /** {@code volume × ptn_por_litro / 1000} — o catálogo é sempre por litro. */
    public static BigDecimal proteinaRecebida(BigDecimal volRecebido, BigDecimal proteinaGL) {
        if (volRecebido == null || proteinaGL == null) return null;
        return volRecebido.multiply(proteinaGL, CONTA).divide(MIL, CONTA);
    }

    /**
     * Diurese em ml/kg/h — {@code diurese / peso / 24}.
     *
     * <p>É a forma em que a diurese se lê em terapia intensiva, e ela <b>precisa
     * do peso</b>: por isso só existe quando há avaliação vinculada. O eroERP
     * guarda só o total em ml.
     */
    public static BigDecimal diuresePorQuiloHora(BigDecimal diureseMl, BigDecimal pesoKg) {
        if (diureseMl == null || !positivo(pesoKg)) return null;
        return diureseMl.divide(pesoKg, CONTA).divide(HORAS_DO_DIA, CONTA);
    }

    /**
     * A média das refeições <b>informadas</b>.
     *
     * <p>Refeição em branco não conta como zero: não ter registrado o lanche da
     * tarde não é o mesmo que o paciente ter recusado o lanche da tarde. O
     * eroERP faz a média sobre as não nulas, e nisso está certo.
     */
    public static BigDecimal mediaIngestaoOral(BigDecimal... refeicoes) {
        var informadas = Stream.of(refeicoes).filter(r -> r != null).toList();
        if (informadas.isEmpty()) return null;

        BigDecimal soma = informadas.stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b, CONTA));
        return dividir(soma, new BigDecimal(informadas.size()));
    }
}
