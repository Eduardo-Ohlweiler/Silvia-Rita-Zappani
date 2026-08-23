package com.nutri.hospitalar.uti.enums;

/**
 * A gravidade de um resultado classificado, atribuída <b>pelo servidor</b>.
 *
 * <p>Existe para tirar a cor do casamento por texto. O eroERP colore o balanço
 * nitrogenado com um hexadecimal cravado no componente e classifica o estado
 * nutricional com {@code texto.contains("adequado")} — as duas coisas quebram
 * quando o rótulo muda.
 *
 * <p>São quatro tons e não três porque a UTI tem escalas de tamanhos diferentes
 * — 6 níveis na adequação de CB, 4 no IMC da OMS, 3 na perda de peso, 2 na
 * depleção — e o {@code FaixaOms} de três valores da pediatria
 * (BAIXA/ADEQUADA/ALTA) não tem onde pôr "grave".
 *
 * <p>{@link #NEUTRO} não é ausência de classificação: é classificação que
 * <b>não</b> carrega juízo clínico, como o preparo escolhido da noradrenalina.
 */
public enum TomResultado {

    NEUTRO,
    ADEQUADO,
    ATENCAO,
    CRITICO
}
