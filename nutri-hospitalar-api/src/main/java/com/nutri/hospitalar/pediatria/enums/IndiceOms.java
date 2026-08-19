package com.nutri.hospitalar.pediatria.enums;

/**
 * Os três índices antropométricos da OMS avaliados, e o rótulo de cada faixa
 * em cada um deles.
 *
 * <p>Os rótulos são exatamente os da planilha {@code Pediatria.xlsx}, incluindo
 * a assimetria de "Adequada" na estatura contra "Peso adequado" e "IMC
 * adequado" nos outros dois — é o texto que a nutricionista reconhece.
 */
public enum IndiceOms {

    PESO_IDADE("Peso para a idade", "Baixo peso", "Peso adequado", "Acima do peso"),
    ESTATURA_IDADE("Estatura para a idade", "Baixa estatura", "Adequada", "Estatura alta"),
    IMC_IDADE("IMC para a idade", "Magreza", "IMC adequado", "Sobrepeso");

    private final String nome;
    private final String rotuloBaixa;
    private final String rotuloAdequada;
    private final String rotuloAlta;

    IndiceOms(String nome, String rotuloBaixa, String rotuloAdequada, String rotuloAlta) {
        this.nome = nome;
        this.rotuloBaixa = rotuloBaixa;
        this.rotuloAdequada = rotuloAdequada;
        this.rotuloAlta = rotuloAlta;
    }

    public String getNome() {
        return nome;
    }

    public String rotulo(FaixaOms faixa) {
        if (faixa == null) return null;
        return switch (faixa) {
            case BAIXA -> rotuloBaixa;
            case ADEQUADA -> rotuloAdequada;
            case ALTA -> rotuloAlta;
        };
    }
}
