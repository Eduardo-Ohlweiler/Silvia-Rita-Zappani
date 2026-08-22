package com.nutri.hospitalar.uti.enums;

/**
 * O que o produto é, e — mais importante — <b>onde ele entra no cálculo</b>.
 *
 * <p>Os três compartilham a estrutura (composição por porção) e se distinguem
 * pelo papel:
 *
 * <ul>
 *   <li>{@link #SUPLEMENTO_ORAL} — consulta e prescrição de complemento;
 *   <li>{@link #MODULO_PROTEICO} — alimenta a sugestão de módulo da dieta
 *       enteral, hoje cravada em código no eroERP;
 *   <li>{@link #INSUMO_ARTESANAL} — compõe a receita do sistema aberto, e por
 *       isso obriga um {@link PapelArtesanal}.
 * </ul>
 *
 * <p>Os dois últimos <b>entram em conta</b>, e é por isso que o banco exige
 * {@code kcal}, {@code proteina_g} e {@code medida_qtd > 0} deles — um módulo
 * sem calorias cadastradas produziria dose nula em silêncio. Suplemento oral é
 * o único que aceita composição incompleta: ali ele é consulta, não operando.
 */
public enum TipoProdutoNutricional {

    SUPLEMENTO_ORAL("Suplemento oral"),
    MODULO_PROTEICO("Módulo proteico"),
    INSUMO_ARTESANAL("Insumo de dieta artesanal");

    private final String descricao;

    TipoProdutoNutricional(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Tipo que entra em cálculo, e por isso exige composição completa. */
    public boolean exigeComposicao() {
        return this != SUPLEMENTO_ORAL;
    }
}
