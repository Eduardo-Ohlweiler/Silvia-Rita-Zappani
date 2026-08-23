package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;

import java.math.BigDecimal;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.MIL;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * As três contas de beira de leito: dose de noradrenalina, balanço nitrogenado e
 * calorias do propofol. Especificação: {@code docs/10} §6.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco. Nenhuma delas persiste
 * nada — são conferências pontuais.
 */
public final class FerramentaClinicaCalculator {

    private FerramentaClinicaCalculator() {}

    /** Miligramas de noradrenalina por ampola. */
    private static final BigDecimal MG_POR_AMPOLA = new BigDecimal("4");

    private static final BigDecimal SEGUNDOS_POR_MINUTO_EM_HORA = new BigDecimal("60");

    /** Nitrogênio por grama de proteína: 1 g de N para cada 6,25 g de PTN. */
    private static final BigDecimal PTN_POR_NITROGENIO = new BigDecimal("6.25");

    /** Fator de conversão da ureia urinária em nitrogênio. */
    private static final BigDecimal UREIA_PARA_NITROGENIO = new BigDecimal("2.14");

    /** Perdas insensíveis — pele, fezes, secreções. O "+4" da planilha. */
    private static final BigDecimal PERDAS_INSENSIVEIS = new BigDecimal("4");

    /** Densidade calórica da emulsão lipídica do propofol a 1 %. */
    private static final BigDecimal KCAL_POR_ML_PROPOFOL = new BigDecimal("1.1");

    /** Horas de infusão do propofol quando o profissional não informa outra. */
    public static final BigDecimal HORAS_PROPOFOL_PADRAO = new BigDecimal("24");

    // ─── Noradrenalina ──────────────────────────────────────────────────

    /**
     * Concentração da bolsa: {@code ampolas × 4 mg × 1000 / volume_do_soro}.
     *
     * <p><b>Os presets "simples 32" e "concentrada 64" da planilha são esta
     * mesma fórmula</b>, não fórmulas separadas: 2 ampolas em 250 ml dão
     * 32 mcg/ml, 4 ampolas em 250 ml dão 64. As notas da planilha confirmam —
     * {@code D1} "considerando diluição em 250ml soro", {@code D3} "2 ampolas",
     * {@code D4} "4 ampolas".
     *
     * <p>Ter uma fórmula só é o que permite ao teste provar que os dois caminhos
     * concordam, e é o que torna os números 32 e 64 auditáveis em vez de
     * mágicos.
     *
     * @param ampolas       número de ampolas de 4 mg
     * @param volumeSoroMl  volume final da bolsa, em ml
     * @return mcg/ml
     */
    public static BigDecimal concentracaoNoradrenalina(BigDecimal ampolas, BigDecimal volumeSoroMl) {
        if (!positivo(ampolas) || !positivo(volumeSoroMl)) return null;
        return ampolas.multiply(MG_POR_AMPOLA, CONTA)
                .multiply(MIL, CONTA)
                .divide(volumeSoroMl, CONTA);
    }

    /**
     * Dose infundida: {@code vazão × concentração / peso / 60}.
     *
     * <p>Confere, peso 70 kg: 25 ml/h a 32 mcg/ml → 0,190476 mcg/kg/min;
     * a 64 → 0,380952; e 4 ampolas em 234 ml (= 68,3761 mcg/ml) a 20 ml/h →
     * 0,325600.
     *
     * <p>O exemplo de 234 ml da planilha ({@code J5}) é 250 − 4 × 4: o volume
     * <b>final</b> da bolsa já contando as ampolas. Vale como lembrete de que
     * quem informa o volume informa o da bolsa pronta.
     *
     * @param vazaoMlH             vazão da bomba, ml/h
     * @param concentracaoMcgMl    concentração da bolsa
     * @return mcg/kg/min
     */
    public static BigDecimal doseNoradrenalina(BigDecimal vazaoMlH, BigDecimal concentracaoMcgMl,
                                               PesoDeTrabalho peso) {
        if (!positivo(vazaoMlH) || !positivo(concentracaoMcgMl) || peso == null) return null;
        return vazaoMlH.multiply(concentracaoMcgMl, CONTA)
                .divide(peso.valorKg(), CONTA)
                .divide(SEGUNDOS_POR_MINUTO_EM_HORA, CONTA);
    }

    // ─── Balanço nitrogenado ────────────────────────────────────────────

    /**
     * Balanço nitrogenado de 24 horas.
     *
     * <pre>
     * N ingerido  = PTN_24h / 6,25
     * N excretado = ureia_urinaria_24h / 2,14 + 4
     * balanço     = ingerido − excretado
     * </pre>
     *
     * <p>Confere: PTN 95 g e ureia 40 g → 15,2 · 22,6916 · −7,4916.
     *
     * <p>O resultado vem <b>classificado</b>: positivo é anabolismo, negativo é
     * catabolismo. O eroERP mostra só o número e o colore com um hexadecimal
     * cravado no componente — quem lê precisa saber o que o sinal significa.
     */
    public static BalancoNitrogenado balancoNitrogenado(BigDecimal proteinaIngerida24hG,
                                                        BigDecimal ureiaUrinaria24hG) {
        if (proteinaIngerida24hG == null || ureiaUrinaria24hG == null) return null;

        BigDecimal ingerido = proteinaIngerida24hG.divide(PTN_POR_NITROGENIO, CONTA);
        BigDecimal excretado = ureiaUrinaria24hG.divide(UREIA_PARA_NITROGENIO, CONTA)
                .add(PERDAS_INSENSIVEIS, CONTA);
        BigDecimal balanco = ingerido.subtract(excretado, CONTA);

        Classificacao classificacao = balanco.signum() > 0
                ? Classificacao.adequada("Balanço positivo · anabolismo")
                : Classificacao.atencao("Balanço negativo · catabolismo");

        return new BalancoNitrogenado(ingerido, excretado, balanco, classificacao);
    }

    /**
     * @param nitrogenioIngerido  g/dia
     * @param nitrogenioExcretado g/dia, já com as perdas insensíveis
     * @param balanco             ingerido − excretado
     */
    public record BalancoNitrogenado(BigDecimal nitrogenioIngerido,
                                     BigDecimal nitrogenioExcretado,
                                     BigDecimal balanco,
                                     Classificacao classificacao) {}

    // ─── Propofol ───────────────────────────────────────────────────────

    /**
     * As calorias que o propofol entrega: {@code vazão × horas × 1,1}.
     *
     * <p>Não é caloria nutricional pedida — é caloria que <b>chega</b> ao
     * paciente por um sedativo, e precisa ser descontada da meta. 1,1 kcal/ml é
     * a emulsão lipídica a 1 %.
     *
     * <p>Confere: 20 ml/h × 24 h → 528 kcal/dia.
     *
     * <p>As <b>24 horas são constante escondida</b> na planilha
     * ({@code Cálculos!B18}); aqui são parâmetro, com 24 como padrão — porque
     * propofol desligado ao meio-dia não entregou 24 horas de caloria.
     */
    public static BigDecimal caloriasPropofol(BigDecimal vazaoMlH, BigDecimal horasInfusao) {
        if (!positivo(vazaoMlH)) return null;
        BigDecimal horas = horasInfusao == null ? HORAS_PROPOFOL_PADRAO : horasInfusao;
        if (!positivo(horas)) return null;
        return vazaoMlH.multiply(horas, CONTA).multiply(KCAL_POR_ML_PROPOFOL, CONTA);
    }
}
