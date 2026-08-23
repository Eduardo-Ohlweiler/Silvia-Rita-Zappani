package com.nutri.hospitalar.uti.enums;

import java.math.BigDecimal;

/**
 * Como a bolsa de noradrenalina foi preparada.
 *
 * <p><b>Os "32" e "64" da planilha não são fórmulas — são presets.</b> Duas
 * ampolas de 4 mg em 250 ml de soro dão 32 mcg/ml; quatro dão 64. A conta é uma
 * só ({@code ampolas × 4 mg × 1000 / soro}), e este enum guarda apenas os dois
 * pares de números que a planilha assume calados nas notas {@code D1} "considerando
 * diluição em 250ml soro", {@code D3} "2 ampolas" e {@code D4} "4 ampolas".
 *
 * <p>Modelar assim é o que torna os 32 e 64 <b>auditáveis</b>: quem quiser
 * conferir vê de onde saíram, e quem prepara diferente escolhe
 * {@link #AMPOLAS_E_SORO} e informa os seus próprios números — sem precisar de
 * uma terceira fórmula.
 *
 * <p>No eroERP isso são dois blocos de entrada separados na mesma tela, pedindo
 * o peso duas vezes ({@code noraPeso} e {@code ampPeso}) e a mesma vazão com
 * dois nomes ({@code noraVol} e {@code ampMlH}).
 */
public enum PreparoNoradrenalina {

    SIMPLES_32("Simples — 2 ampolas em 250 ml (32 mcg/ml)", "2", "250"),
    CONCENTRADA_64("Concentrada — 4 ampolas em 250 ml (64 mcg/ml)", "4", "250"),

    /** O preparo do serviço: as ampolas e o volume vêm do formulário. */
    AMPOLAS_E_SORO("Outro preparo — informar ampolas e soro", null, null);

    private final String descricao;
    private final BigDecimal ampolas;
    private final BigDecimal volumeSoroMl;

    PreparoNoradrenalina(String descricao, String ampolas, String volumeSoroMl) {
        this.descricao = descricao;
        this.ampolas = ampolas == null ? null : new BigDecimal(ampolas);
        this.volumeSoroMl = volumeSoroMl == null ? null : new BigDecimal(volumeSoroMl);
    }

    public String getDescricao() {
        return descricao;
    }

    /** Nulo em {@link #AMPOLAS_E_SORO}: ali quem informa é o formulário. */
    public BigDecimal getAmpolas() {
        return ampolas;
    }

    public BigDecimal getVolumeSoroMl() {
        return volumeSoroMl;
    }

    public boolean exigeAmpolasESoro() {
        return this == AMPOLAS_E_SORO;
    }
}
