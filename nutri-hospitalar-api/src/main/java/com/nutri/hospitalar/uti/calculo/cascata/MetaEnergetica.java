package com.nutri.hospitalar.uti.calculo.cascata;

import com.nutri.hospitalar.uti.enums.OrigemValor;

import java.math.BigDecimal;

/**
 * A meta calórica do dia, com a procedência colada.
 *
 * <p>Na planilha, <b>nada liga {@code Necessidades!B5} a {@code Contínuo!P4}</b>:
 * a meta é redigitada à mão em cada aba (defeito 14 de {@code docs/10} §11).
 * Então a aba da dieta pode estar perseguindo uma meta que a aba das
 * necessidades já não recomenda, e a tela não tem como mostrar isso.
 *
 * <p>Aqui a dieta recebe a meta como tipo, e a tela exibe de onde ela veio —
 * "da faixa da fase", "alvo informado" ou "protocolo de obesidade".
 *
 * @param kcalDia meta em kcal/dia
 * @param origem  o ramo de {@code NecessidadeCalculator} que a produziu
 */
public record MetaEnergetica(BigDecimal kcalDia, OrigemValor origem) {

    public MetaEnergetica {
        if (kcalDia == null || kcalDia.signum() < 0)
            throw new IllegalArgumentException("Meta energética não pode ser negativa");
        if (origem == null)
            throw new IllegalArgumentException("Meta energética precisa declarar a origem");
    }

    public String descricaoOrigem() {
        return origem.getDescricao();
    }
}
