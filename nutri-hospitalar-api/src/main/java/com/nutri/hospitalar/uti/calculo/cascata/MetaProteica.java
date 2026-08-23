package com.nutri.hospitalar.uti.calculo.cascata;

import com.nutri.hospitalar.uti.enums.OrigemValor;

import java.math.BigDecimal;

/**
 * A meta proteica do dia, com a procedência colada. Mesma razão de
 * {@link MetaEnergetica}.
 *
 * <p>A origem importa mais aqui do que na energia, porque a proteína tem três
 * caminhos que se sobrepõem: a faixa da fase, o protocolo de obesidade (que usa
 * <b>peso ideal</b>, não atual) e a terapia renal substitutiva — que
 * <b>substitui</b> os dois. Sem dizer qual venceu, o número é inauditável.
 *
 * @param gramasDia meta em g/dia
 * @param origem    o ramo que a produziu
 */
public record MetaProteica(BigDecimal gramasDia, OrigemValor origem) {

    public MetaProteica {
        if (gramasDia == null || gramasDia.signum() < 0)
            throw new IllegalArgumentException("Meta proteica não pode ser negativa");
        if (origem == null)
            throw new IllegalArgumentException("Meta proteica precisa declarar a origem");
    }

    public String descricaoOrigem() {
        return origem.getDescricao();
    }
}
