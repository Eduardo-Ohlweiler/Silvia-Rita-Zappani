package com.nutri.hospitalar.uti.enums;

/**
 * O dicotômico étnico da equação de peso estimado de Chumlea 1988, que tem oito
 * ramos: sexo × etnia × faixa de idade. Ver {@code docs/10} §2.2.
 *
 * <p><b>Mora aqui e não em {@code pessoa}, de propósito.</b> Não é característica
 * cadastral: é um parâmetro de uma equação de 1988, coletado no momento da
 * avaliação e válido só para ela. Gravá-lo na pessoa seria erro de modelagem — e
 * guardar dado sensível sob a LGPD sem contrapartida de uso.
 */
public enum EtniaChumlea {

    BRANCA("Branca"),
    NEGRA("Negra");

    private final String descricao;

    EtniaChumlea(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
