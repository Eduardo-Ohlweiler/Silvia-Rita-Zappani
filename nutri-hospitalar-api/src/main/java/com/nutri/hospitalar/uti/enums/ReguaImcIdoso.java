package com.nutri.hospitalar.uti.enums;

import java.math.BigDecimal;

/**
 * Qual régua de classificação de IMC usar <b>no idoso</b>. Ver {@code docs/10} §2.6.
 *
 * <p><b>As duas são literatura publicada, e discordam em toda a faixa central.</b>
 * Um IMC de 22,5 é <i>eutrofia</i> por Lipschitz e <i>baixo peso</i> pela OPAS;
 * 27,5 é <i>excesso de peso</i> por Lipschitz e <i>eutrofia</i> pela OPAS. Não é
 * detalhe de rótulo: é o diagnóstico nutricional mudando de lado.
 *
 * <ul>
 *   <li>{@link #LIPSCHITZ_1994} — Lipschitz DA. <i>Screening for nutritional
 *       status in the elderly.</i> Prim Care. 1994;21(1):55-67 (PMID 8197257).
 *       Três faixas: baixo peso &lt; 22 · eutrofia 22 a 27 · excesso &gt; 27.
 *       É a régua adotada pelo SISVAN / Ministério da Saúde no Brasil.
 *   <li>{@link #OPAS_2002} — derivada do estudo SABE, sete países da América
 *       Latina e Caribe ({@code Estimativas!C43:C47} da planilha de origem).
 *       Quatro faixas: baixo peso &lt; 23 · eutrofia 23 a 28 · excesso 28 a 30 ·
 *       obesidade a partir de 30 — é a única das duas que separa excesso de peso
 *       de obesidade no idoso.
 * </ul>
 *
 * <p>O padrão é {@link #LIPSCHITZ_1994}, e ele está declarado <b>uma vez</b>, em
 * {@link #padrao()}: quem precisa dele — a entrada do cálculo, o service que
 * grava e o seletor da tela — lê daqui.
 *
 * <p><b>Os rótulos das duas colidem</b> ("Baixo peso", "Eutrofia", "Excesso de
 * peso" servem às duas), e é por isso que a régua é coluna no banco e aparece ao
 * lado do rótulo na tela e no papel. Qualquer agregação sobre
 * {@code classif_imc_idoso} tem de agrupar por {@code (regua_imc_idoso,
 * classif_imc_idoso)} — somar as duas numa barra só somaria dois significados.
 */
public enum ReguaImcIdoso {

    LIPSCHITZ_1994("Lipschitz 1994", "22", "27"),
    OPAS_2002("OPAS 2002", "23", "28");

    private final String descricao;
    private final BigDecimal imcEutrofiaMin;
    private final BigDecimal imcEutrofiaMax;

    ReguaImcIdoso(String descricao, String imcEutrofiaMin, String imcEutrofiaMax) {
        this.descricao = descricao;
        this.imcEutrofiaMin = new BigDecimal(imcEutrofiaMin);
        this.imcEutrofiaMax = new BigDecimal(imcEutrofiaMax);
    }

    public String getDescricao() {
        return descricao;
    }

    /** Piso da eutrofia: 22 (Lipschitz) · 23 (OPAS). Faixa fechada à esquerda. */
    public BigDecimal getImcEutrofiaMin() {
        return imcEutrofiaMin;
    }

    /**
     * Teto da eutrofia: 27 (Lipschitz) · 28 (OPAS). O valor exato já é a faixa de
     * cima — é daqui que sai o limite superior da <b>faixa de peso do idoso</b>,
     * e é o que garante que faixa de peso e classificação nunca discordem.
     */
    public BigDecimal getImcEutrofiaMax() {
        return imcEutrofiaMax;
    }

    /** O padrão do módulo, declarado UMA vez. */
    public static ReguaImcIdoso padrao() {
        return LIPSCHITZ_1994;
    }
}
