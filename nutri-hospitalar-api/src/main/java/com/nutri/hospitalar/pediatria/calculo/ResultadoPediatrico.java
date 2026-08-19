package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;

import java.math.BigDecimal;

/**
 * O resultado de um cálculo pediátrico.
 *
 * <p>Cada bloco traz, ao lado dos números, o <b>motivo</b> de eles estarem
 * ausentes quando estão. A planilha e o eroERP devolvem vazio silencioso: a
 * criança de 40 meses simplesmente não recebe VET, sem uma palavra. Um traço
 * mudo na tela faz o profissional achar que o sistema falhou — e o motivo é
 * informação clínica ("acima de 35 meses"), não mensagem de erro.
 *
 * <p>Os três blocos correspondem às abas da tela de cálculo.
 */
public record ResultadoPediatrico(

        // ─── Estado nutricional — OMS ───────────────────────────────────
        BigDecimal imc,
        String motivoImc,

        FaixaOms pesoIdade,
        FaixaOms estaturaIdade,
        FaixaOms imcIdade,
        /** Por que as classificações não saíram. Nulo quando ao menos uma saiu. */
        String motivoEstadoNutricional,

        // ─── Necessidades — DRIs 2002 ───────────────────────────────────
        BigDecimal vet,
        String motivoVet,

        BigDecimal proteinaNecessidade,
        String motivoProteina,

        // ─── Dieta láctea prescrita ─────────────────────────────────────
        BigDecimal vezesDia,
        BigDecimal volumeTotal,
        BigDecimal caloriasTotais,
        BigDecimal proteinaTotal,
        BigDecimal percCalorico,
        BigDecimal percProteico,
        String motivoDieta
) {}
