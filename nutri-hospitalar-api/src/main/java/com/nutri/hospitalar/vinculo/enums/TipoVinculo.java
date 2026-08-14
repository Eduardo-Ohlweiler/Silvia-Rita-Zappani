package com.nutri.hospitalar.vinculo.enums;

/**
 * Papel de uma pessoa em relação à outra.
 *
 * <p>Só responsável e dependente são inversos entre si; cônjuge e familiar são
 * simétricos — valem igual dos dois lados.
 */
public enum TipoVinculo {

    RESPONSAVEL("Responsável"),
    DEPENDENTE("Dependente"),
    CONJUGE("Cônjuge"),
    FAMILIAR("Familiar");

    private final String descricao;

    TipoVinculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Como o vínculo é visto pelo outro lado da relação.
     *
     * <p>É o que permite uma linha só servir os dois cadastros: quem é
     * responsável de alguém aparece, no cadastro desse alguém, como
     * dependente.
     */
    public TipoVinculo inverso() {
        return switch (this) {
            case RESPONSAVEL -> DEPENDENTE;
            case DEPENDENTE -> RESPONSAVEL;
            default -> this;
        };
    }
}
