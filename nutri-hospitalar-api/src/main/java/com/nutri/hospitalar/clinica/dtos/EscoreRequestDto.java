package com.nutri.hospitalar.clinica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O que a tela manda para ver o escore <b>antes de salvar</b>.
 *
 * <p>Não há {@code @PastOrPresent} na data nem {@code @NotEmpty} nas respostas, e
 * isso é desenho: esta superfície recalcula sozinha a cada pausa de digitação, e
 * o formulário passa quase toda a vida pela metade. <b>Incompleto é estado, não
 * erro</b> — a resposta vem 200, com escore nulo e o motivo escrito. É a lição da
 * fatia 12.1, onde três toasts vermelhos empilhavam enquanto a máscara de
 * centavos atravessava {@code 0,05 · 0,53 · 5,30}.
 *
 * @param pacienteId necessário para a idade da NRS-2002, que o sistema calcula em
 *                   vez de perguntar. Anulável: sem ele o escore daquela escala
 *                   não conclui, e diz por quê
 */
public record EscoreRequestDto(

        @NotNull(message = "Informe o modelo de ficha")
        UUID modeloId,

        UUID pacienteId,

        LocalDate dataPreenchimento,

        @Valid
        List<RespostaFichaDto> respostas
) {}
