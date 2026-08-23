package com.nutri.hospitalar.uti.calculo.cascata;

import com.nutri.hospitalar.uti.calculo.UtiMatematica;
import com.nutri.hospitalar.uti.enums.OrigemValor;

import java.math.BigDecimal;

/**
 * A altura que o resto do cálculo vai usar, com a procedência colada — mesma
 * razão de {@link PesoDeTrabalho}: na planilha ela está digitada em quatro
 * células (1,68 · 1,75 · 1,72 · 1,60).
 *
 * <p><b>Guarda metros, sempre.</b> A entrada da tela é em centímetros e o IMC
 * pede metros; converter em um lugar só elimina a classe de erro em que o valor
 * certo é usado na unidade errada.
 *
 * @param valorM altura em metros, sempre maior que zero
 * @param origem informada, ou estimada por Chumlea 1985
 */
public record Altura(BigDecimal valorM, OrigemValor origem) {

    public Altura {
        if (valorM == null || valorM.signum() <= 0)
            throw new IllegalArgumentException("Altura precisa ser maior que zero");
        if (origem == null)
            throw new IllegalArgumentException("Altura precisa declarar a origem");
    }

    public static Altura informadaEmCentimetros(BigDecimal cm) {
        return new Altura(UtiMatematica.dividir(cm, UtiMatematica.CEM), OrigemValor.INFORMADO);
    }

    public static Altura estimadaEmCentimetros(BigDecimal cm) {
        return new Altura(UtiMatematica.dividir(cm, UtiMatematica.CEM),
                OrigemValor.ALTURA_ESTIMADA_CHUMLEA);
    }

    public BigDecimal emCentimetros() {
        return valorM.multiply(UtiMatematica.CEM, UtiMatematica.CONTA);
    }

    /** O denominador do IMC. */
    public BigDecimal aoQuadrado() {
        return valorM.multiply(valorM, UtiMatematica.CONTA);
    }

    public String descricaoOrigem() {
        return origem.getDescricao();
    }
}
