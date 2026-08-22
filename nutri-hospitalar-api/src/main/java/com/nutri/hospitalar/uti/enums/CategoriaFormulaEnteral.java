package com.nutri.hospitalar.uti.enums;

/**
 * Categoria terapêutica da fórmula enteral, como a planilha
 * {@code Facilita Nutri na UTI} agrupa a aba {@code Dietas consulta}.
 *
 * <p><b>Não entra em cálculo nenhum.</b> É rótulo de escolha: quem prescreve
 * filtra por "peptídica" antes de comparar densidade e proteína. Por isso é
 * anulável na entidade — produto sem categoria declarada continua utilizável.
 *
 * <p>O valor gravado é a constante; o acento vive só no {@link #getDescricao()}.
 * Ver a migration {@code 021}, que corrigiu o inverso.
 */
public enum CategoriaFormulaEnteral {

    PADRAO("Padrão"),
    HIPERPROTEICA("Hiperproteica"),
    DIABETES("Diabetes"),
    PEPTIDICA("Peptídica"),
    IMUNOMODULADORA("Imunomoduladora"),
    RENAL("Renal"),
    HEPATICA("Hepática");

    private final String descricao;

    CategoriaFormulaEnteral(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
