package com.nutri.hospitalar.clinica.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nutri.hospitalar.uti.enums.TomResultado;

/**
 * Um resultado classificado: o rótulo que o profissional lê e o tom que a tela
 * usa para colorir.
 *
 * <p>O {@link TomResultado} é reusado da UTI de propósito, e isso não contraria
 * a regra de não importar um módulo dentro do outro. O que aquela regra proíbe é
 * reusar <b>cálculo</b> — lá a armadilha era a unidade declarada do catálogo, que
 * difere entre os módulos e faria a conta errar por um fator de dez. Aqui o enum
 * é <b>vocabulário de exibição</b>, sem unidade e sem regra clínica: são as
 * quatro gravidades que o {@code TResult} do front já conhece, num tipo só.
 * Duplicá-lo criaria dois enums para a mesma coisa, e o quinto tom nasceria em um
 * deles.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassificacaoEscoreDto(String rotulo, TomResultado tom) {

    /** Classificação sem juízo clínico — um estado, não uma gravidade. */
    public static ClassificacaoEscoreDto neutra(String rotulo) {
        return new ClassificacaoEscoreDto(rotulo, TomResultado.NEUTRO);
    }

    public static ClassificacaoEscoreDto adequada(String rotulo) {
        return new ClassificacaoEscoreDto(rotulo, TomResultado.ADEQUADO);
    }

    public static ClassificacaoEscoreDto atencao(String rotulo) {
        return new ClassificacaoEscoreDto(rotulo, TomResultado.ATENCAO);
    }

    public static ClassificacaoEscoreDto critica(String rotulo) {
        return new ClassificacaoEscoreDto(rotulo, TomResultado.CRITICO);
    }
}
