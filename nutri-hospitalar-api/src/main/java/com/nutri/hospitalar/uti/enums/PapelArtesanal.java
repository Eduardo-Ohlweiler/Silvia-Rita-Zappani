package com.nutri.hospitalar.uti.enums;

/**
 * O papel do insumo na receita da dieta artesanal — sistema aberto.
 *
 * <p><b>Os quatro papéis são fixos; quem os ocupa é o catálogo.</b> Essa é a
 * divisão que mata o hardcode do eroERP
 * ({@code calculoSistemaAberto.ts:6-11}, quatro produtos em constante) sem
 * inventar comportamento clínico: a lógica de cada papel é específica — a base
 * se auto-sugere pelo VET, o carboidrato entra fora do kcal da base — e
 * generalizar para "quatro produtos quaisquer" seria invenção nossa.
 *
 * <p>Então a nutricionista troca o produto de cada papel, e não o papel.
 */
public enum PapelArtesanal {

    BASE("Base"),
    CARBOIDRATO("Carboidrato"),
    PROTEINA("Proteína"),
    LIPIDIO("Lipídio");

    private final String descricao;

    PapelArtesanal(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
