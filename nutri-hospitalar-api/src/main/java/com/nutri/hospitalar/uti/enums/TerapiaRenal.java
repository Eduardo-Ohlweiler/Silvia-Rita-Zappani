package com.nutri.hospitalar.uti.enums;

import java.math.BigDecimal;

/**
 * Terapia renal substitutiva — quando presente, ela <b>substitui</b> a meta
 * proteica: a da faixa da fase e a do protocolo de obesidade.
 *
 * <p><b>Com uma exceção, e ela é deliberada:</b> o {@code g/kg alvo} digitado
 * vence a terapia renal, porque alvo digitado é conduta explícita de quem
 * prescreve. Este javadoc afirmava a substituição sem ressalva, e era um dos
 * cinco textos que prometiam o oposto do que o código fazia — com a
 * consequência de o sistema adotar 84,38 g em vez de 129,82 e declarar a meta
 * batida, calado. Hoje a preterição é <b>publicada</b>, não escondida: ver
 * {@code ResultadoUti.Necessidades#alvoProteicoPreteriuTerapiaRenal} e
 * {@code docs/10} §3.2.
 *
 * <p>A meta que sai daqui é <b>valor único</b>, não ponto de faixa: origem
 * {@link OrigemValor#META_TERAPIA_RENAL}, sem posição. Publicá-la como
 * {@code META_POR_FAIXA} fazia a tela escrever "136,0 · da faixa da fase"
 * debaixo de "Proteína — máximo 102,0".
 */
public enum TerapiaRenal {

    NENHUMA("Nenhuma", null),
    HEMODIALISE_INTERMITENTE("Hemodiálise intermitente", new BigDecimal("1.8")),
    HEMODIALISE_CONTINUA("Hemodiálise contínua", new BigDecimal("2.0"));

    private final String descricao;

    /** g/kg/dia. Nulo em {@link #NENHUMA} — ausência, não zero. */
    private final BigDecimal proteinaGKg;

    TerapiaRenal(String descricao, BigDecimal proteinaGKg) {
        this.descricao = descricao;
        this.proteinaGKg = proteinaGKg;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getProteinaGKg() {
        return proteinaGKg;
    }
}
