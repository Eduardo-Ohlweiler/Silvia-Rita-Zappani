package com.nutri.hospitalar.pediatria.dtos;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;

/**
 * Uma classificação da OMS: a faixa, para o front colorir, e o rótulo, para
 * ele escrever.
 *
 * <p>Os dois viajam juntos de propósito. "Baixo peso", "Baixa estatura" e
 * "Magreza" são a MESMA faixa em índices diferentes — mandar só o texto
 * obrigaria o front a adivinhar a cor por substring, que é o que o eroERP
 * fazia (`texto.contains("adequado")`).
 */
public record ClassificacaoDto(
        FaixaOms faixa,
        String rotulo
) {}
