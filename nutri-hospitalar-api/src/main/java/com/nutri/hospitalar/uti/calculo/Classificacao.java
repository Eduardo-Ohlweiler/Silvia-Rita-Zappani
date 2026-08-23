package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.uti.enums.TomResultado;

/**
 * Um resultado classificado: o rótulo que o profissional lê e o tom que a tela
 * usa para colorir.
 *
 * <p><b>O tom vem do servidor.</b> É o que tira a cor do casamento por texto —
 * o eroERP classifica com {@code texto.contains("adequado")} e colore o balanço
 * nitrogenado com um hexadecimal cravado no componente, e as duas coisas quebram
 * quando o rótulo muda de palavra.
 *
 * @param rotulo o texto exato, como {@code docs/10} o define
 * @param tom    a gravidade, para a tela decidir a cor sem ler o texto
 */
public record Classificacao(String rotulo, TomResultado tom) {

    public static Classificacao neutra(String rotulo) {
        return new Classificacao(rotulo, TomResultado.NEUTRO);
    }

    public static Classificacao adequada(String rotulo) {
        return new Classificacao(rotulo, TomResultado.ADEQUADO);
    }

    public static Classificacao atencao(String rotulo) {
        return new Classificacao(rotulo, TomResultado.ATENCAO);
    }

    public static Classificacao critica(String rotulo) {
        return new Classificacao(rotulo, TomResultado.CRITICO);
    }
}
