package com.nutri.hospitalar.uti.calculo.cascata;

import java.math.BigDecimal;

/**
 * O volume total de dieta do dia — o VT.
 *
 * <p>É a saída da dieta enteral e a <b>entrada</b> da hidratação: a água que já
 * vem na dieta desconta da água extra a ofertar. Na planilha esse elo é
 * digitado, e em dois pontos ele nem existe: {@code Hidratação!D21} e
 * {@code E21} auto-referenciam uma célula vazia da própria aba quando deveriam
 * ler o volume da dieta artesanal (defeito 9 de {@code docs/10} §11), e a água
 * extra simplesmente não desconta nada.
 *
 * @param ml volume total em ml/dia
 * @param modoDescricao como ele foi obtido, para a tela ("1364 ml · 62 ml/h × 22 h")
 */
public record VolumeDieta(BigDecimal ml, String modoDescricao) {

    public VolumeDieta {
        if (ml == null || ml.signum() < 0)
            throw new IllegalArgumentException("Volume de dieta não pode ser negativo");
    }
}
