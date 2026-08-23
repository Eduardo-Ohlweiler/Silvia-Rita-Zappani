package com.nutri.hospitalar.uti.enums;

/**
 * Qual coluna de ajuste de CB e CP pelo IMC usar. Ver {@code docs/10} §2.9.
 *
 * <p><b>As duas são literatura publicada, e discordam em uma faixa só: IMC &lt;
 * 18,5.</b> Nas outras quatro faixas o ajuste é idêntico.
 *
 * <ul>
 *   <li>{@link #ADULTO_SAUDAVEL} — Gonzalez et al., <i>Am J Clin Nutr</i>
 *       2021;113(6):1679-87, Tabelas 5 e 6: <b>soma</b> centímetro no magro
 *       (CP +4; CB +2 M / +3 H), corrigindo o viés da medida crua.
 *   <li>{@link #POPULACAO_CLINICA} — a orientação GLIM (Barazzoni et al.,
 *       <i>Clin Nutr</i> 2022;41(6):1425-33) não incorpora esse acréscimo: em
 *       doença catabólica com suspeita de perda de peso ou de massa, somar
 *       centímetro <b>mascara</b> a depleção.
 * </ul>
 *
 * <p>O padrão é {@link #POPULACAO_CLINICA} porque o módulo é de UTI, e porque é
 * o lado conservador: erra para o diagnóstico, não contra ele.
 */
public enum PopulacaoReferencia {

    POPULACAO_CLINICA("População clínica"),
    ADULTO_SAUDAVEL("Adulto saudável");

    private final String descricao;

    PopulacaoReferencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** O acréscimo em IMC &lt; 18,5 só existe na coluna do adulto saudável. */
    public boolean somaNoMagro() {
        return this == ADULTO_SAUDAVEL;
    }
}
