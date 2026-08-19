package com.nutri.hospitalar.pessoa.enums;

/**
 * Sexo biológico, usado pelos cálculos que dependem dele.
 *
 * <p>Não é campo administrativo: as curvas de crescimento da OMS são tabelas
 * SEPARADAS por sexo, e classificar uma criança pela tabela errada é
 * diagnóstico errado. Ver {@code docs/09-calculos-pediatria.md} §4.
 *
 * <p>Opcional em {@code Pessoa} — pessoa jurídica não tem, e os cadastros
 * anteriores à coluna também não. Por isso toda tela de cálculo mantém o seu
 * próprio campo de sexo: o cadastro preenche quando sabe, o profissional
 * completa quando falta.
 */
public enum Sexo {
    MASCULINO,
    FEMININO
}
