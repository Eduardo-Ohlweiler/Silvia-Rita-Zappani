package com.nutri.hospitalar.pediatria.enums;

/**
 * Faixa de classificação da OMS: onde o valor medido cai em relação aos
 * percentis P15 e P85 da idade e do sexo.
 *
 * <p>Os cortes vêm da planilha {@code Pediatria.xlsx} (células B8, B9 e B11) e
 * são <b>assimétricos</b>: P15 e P85 são ambos INCLUSIVOS na faixa adequada, e
 * só {@code > P85} sobe de faixa. Ver {@code docs/09-calculos-pediatria.md} §4.1.
 *
 * <p>Guardamos a faixa, não o rótulo — "Baixo peso", "Baixa estatura" e
 * "Magreza" são a MESMA faixa em índices diferentes, e quem sabe o índice é a
 * coluna. Ver {@link IndiceOms}.
 */
public enum FaixaOms {
    BAIXA,
    ADEQUADA,
    ALTA
}
