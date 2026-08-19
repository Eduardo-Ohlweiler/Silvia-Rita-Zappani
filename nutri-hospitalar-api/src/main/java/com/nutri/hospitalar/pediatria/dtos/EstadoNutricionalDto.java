package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;

/**
 * Bloco do estado nutricional pela OMS — a primeira aba da tela de cálculo.
 *
 * @param motivoImc    por que não há IMC. Nulo quando há.
 * @param motivo       por que não há classificação nenhuma — sexo ausente,
 *                     idade ausente, ou idade fora de 0 a 60 meses. Nulo
 *                     quando ao menos uma classificação saiu.
 */
public record EstadoNutricionalDto(
        BigDecimal imc,
        String motivoImc,
        ClassificacaoDto pesoIdade,
        ClassificacaoDto estaturaIdade,
        ClassificacaoDto imcIdade,
        String motivo
) {}
