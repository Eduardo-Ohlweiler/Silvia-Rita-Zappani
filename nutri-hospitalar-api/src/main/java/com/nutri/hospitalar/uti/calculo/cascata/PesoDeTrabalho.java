package com.nutri.hospitalar.uti.calculo.cascata;

import com.nutri.hospitalar.uti.enums.OrigemValor;

import java.math.BigDecimal;

/**
 * O peso que o resto do cálculo vai usar — <b>com a sua procedência colada</b>.
 *
 * <p>Este tipo existe por um motivo concreto. A planilha tem o peso do paciente
 * digitado em <b>oito células independentes</b> (85 · 60 · 62 · 68 · 68 · 72 ·
 * 65 · 75 · 70) e nada liga uma à outra: a aba das necessidades e a aba da dieta
 * podem estar calculando sobre pesos diferentes sem que nada acuse. É o defeito
 * 15 de {@code docs/10} §11, e não é erro de fórmula — é ausência de cascata.
 *
 * <p>A resposta é de tipo, não de disciplina: <b>nenhuma etapa do cálculo aceita
 * um {@code BigDecimal} de peso.</b> Todas aceitam {@code PesoDeTrabalho}, e só
 * {@code AntropometriaCalculator} sabe construí-lo. "A aba da dieta usou um peso
 * diferente da aba das necessidades" deixa de ser um bug que se possa escrever.
 *
 * <p>E a origem não é enfeite: ela sobe até a tela, por extenso — <i>"peso
 * estimado · Rabito 2008"</i> — para que ninguém precise adivinhar sobre que
 * número a prescrição foi feita.
 *
 * @param valorKg peso em quilos, sempre maior que zero
 * @param origem  de onde ele veio
 */
public record PesoDeTrabalho(BigDecimal valorKg, OrigemValor origem) {

    public PesoDeTrabalho {
        if (valorKg == null || valorKg.signum() <= 0)
            throw new IllegalArgumentException("Peso de trabalho precisa ser maior que zero");
        if (origem == null)
            throw new IllegalArgumentException("Peso de trabalho precisa declarar a origem");
    }

    public static PesoDeTrabalho informado(BigDecimal kg) {
        return new PesoDeTrabalho(kg, OrigemValor.INFORMADO);
    }

    public String descricaoOrigem() {
        return origem.getDescricao();
    }
}
