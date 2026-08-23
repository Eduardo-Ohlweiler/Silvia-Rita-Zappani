package com.nutri.hospitalar.uti.enums;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * Percentual do peso corporal por segmento amputado — Osterkamp 1995,
 * {@code Estimativas!D16:E24}. Ver {@code docs/10} §2.5.
 *
 * <p><b>Os segmentos se contêm, e é por isso que este enum tem lógica.</b> A
 * própria tabela mostra: 0,7 + 1,6 = 2,3 (mão + antebraço = antebraço com mão) e
 * 2,7 + 2,3 = 5,0 (braço + antebraço com mão = membro superior). Selecionar
 * "Membro superior" <b>e</b> "Mão" desconta a mão duas vezes e devolve um peso
 * menor que o real — em silêncio.
 *
 * <p>O membro inferior contém a perna e o pé por anatomia, ainda que a soma não
 * feche (16,0 contra 4,4 + 1,5): a diferença é a coxa, que a tabela não lista
 * isolada.
 *
 * <p>Quem valida a combinação é o calculador, que recusa nomeando os dois
 * segmentos sobrepostos.
 */
public enum SegmentoAmputado {

    MAO("Mão", "0.7"),
    ANTEBRACO("Antebraço", "1.6"),
    ANTEBRACO_MAO("Antebraço com mão", "2.3"),
    BRACO("Braço", "2.7"),
    MEMBRO_SUPERIOR("Membro superior", "5.0"),
    PE("Pé", "1.5"),
    PERNA("Perna", "4.4"),
    MEMBRO_INFERIOR("Membro inferior", "16.0");

    private final String descricao;
    private final BigDecimal percentual;

    SegmentoAmputado(String descricao, String percentual) {
        this.descricao = descricao;
        this.percentual = new BigDecimal(percentual);
    }

    public String getDescricao() {
        return descricao;
    }

    /** % do peso corporal total. */
    public BigDecimal getPercentual() {
        return percentual;
    }

    /**
     * Os segmentos que este já inclui.
     *
     * <p>Em método e não em campo porque uma constante de enum não pode
     * referenciar outra no construtor.
     */
    public Set<SegmentoAmputado> contidos() {
        return switch (this) {
            case ANTEBRACO_MAO   -> EnumSet.of(MAO, ANTEBRACO);
            case MEMBRO_SUPERIOR -> EnumSet.of(BRACO, ANTEBRACO_MAO, ANTEBRACO, MAO);
            case MEMBRO_INFERIOR -> EnumSet.of(PERNA, PE);
            default              -> EnumSet.noneOf(SegmentoAmputado.class);
        };
    }

    /** Este segmento já desconta o outro? */
    public boolean contem(SegmentoAmputado outro) {
        return contidos().contains(outro);
    }
}
