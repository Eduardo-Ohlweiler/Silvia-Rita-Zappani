package com.nutri.hospitalar.uti.enums;

import java.math.BigDecimal;

/**
 * A janela de tempo em que a perda de peso ocorreu, e os cortes de Blackburn
 * 1977 correspondentes ({@code Estimativas!A36:C40}). Ver {@code docs/10} §2.7.
 *
 * <p><b>A janela é entrada obrigatória, não detalhe.</b> Perder 5 % em uma
 * semana é grave; perder 5 % em seis meses ainda é moderado. O mesmo percentual
 * muda de diagnóstico conforme o tempo — e o eroERP fixa a janela em "1 mês"
 * apesar de ter as quatro implementadas ({@code tabelas.ts:77-91}).
 *
 * <table>
 *   <caption>Cortes por janela</caption>
 *   <tr><th>Janela</th><th>Perda moderada</th><th>Perda grave</th></tr>
 *   <tr><td>1 semana</td><td>1 – 2 %</td><td>≥ 2 %</td></tr>
 *   <tr><td>1 mês</td><td>&lt; 5 %</td><td>≥ 5 %</td></tr>
 *   <tr><td>3 meses</td><td>&lt; 7,5 %</td><td>≥ 7,5 %</td></tr>
 *   <tr><td>6 meses</td><td>&lt; 10 %</td><td>≥ 10 %</td></tr>
 * </table>
 *
 * <p>Só a janela de uma semana tem <b>piso</b> para a faixa moderada: abaixo de
 * 1 % em sete dias a tabela não classifica. Nas outras três, qualquer perda
 * abaixo do corte grave já é moderada.
 */
public enum JanelaPerdaPeso {

    UMA_SEMANA("1 semana",  "1.0",  "2.0"),
    UM_MES("1 mês",         "0.0",  "5.0"),
    TRES_MESES("3 meses",   "0.0",  "7.5"),
    SEIS_MESES("6 meses",   "0.0", "10.0");

    private final String descricao;

    /** Piso da faixa moderada, inclusive. Zero significa "qualquer perda". */
    private final BigDecimal perdaModeradaMin;

    /** A partir daqui, inclusive, a perda é grave. */
    private final BigDecimal perdaGrave;

    JanelaPerdaPeso(String descricao, String perdaModeradaMin, String perdaGrave) {
        this.descricao = descricao;
        this.perdaModeradaMin = new BigDecimal(perdaModeradaMin);
        this.perdaGrave = new BigDecimal(perdaGrave);
    }

    public String getDescricao()              { return descricao; }
    public BigDecimal getPerdaModeradaMin()   { return perdaModeradaMin; }
    public BigDecimal getPerdaGrave()         { return perdaGrave; }
}
