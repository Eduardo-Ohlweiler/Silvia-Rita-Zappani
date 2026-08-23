package com.nutri.hospitalar.uti.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Mesmos campos do cadastro — a avaliação é editada por inteiro, como é
 * gravada.
 *
 * <p>Uma avaliação a alterar: quem, quando, as entradas do cálculo e a
 * observação.
 *
 * <p><b>As entradas do cálculo vêm compostas, não copiadas.</b> É o mesmo
 * {@link CalculoUtiRequestDto} que a calculadora manda para {@code /uti/calculo},
 * e isso não é economia de digitação: é o que garante que gravar e calcular
 * nunca divirjam. Duas listas de campos paralelas divergiriam na primeira vez
 * que alguém acrescentasse um só de um lado.
 *
 * <p><b>Nenhum resultado é aceito.</b> O servidor recalcula a partir das
 * entradas e grava os seus próprios números — mandar um {@code imc} no corpo não
 * tem efeito.
 *
 * @param profissionalId opcional: nem toda avaliação tem profissional
 *                       identificado, e ele é {@code pessoa}, não usuário
 */
public record AvaliacaoUtiUpdateDto(

        @NotNull(message = "Informe o paciente")
        UUID pacienteId,

        UUID profissionalId,

        @NotNull(message = "Informe a data da avaliação")
        @PastOrPresent(message = "A data da avaliação não pode ser no futuro")
        LocalDate dataAvaliacao,

        @NotNull(message = "Informe as entradas do cálculo")
        @Valid
        CalculoUtiRequestDto calculo,

        @Size(max = 5000, message = "No máximo 5000 caracteres")
        String observacao
) {}
