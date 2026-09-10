package com.nutri.hospitalar.clinica.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param doSistema modelo global, visível a todo cliente e editável por nenhum.
 *                  A tela usa isto para abrir em somente leitura e oferecer
 *                  Clonar em vez de Salvar — um botão que existe e recusa é pior
 *                  que um botão que não existe.
 * @param escoreCodigo qual escala pontuada o modelo aplica, ou nulo para o
 *                  questionário descritivo. <b>Não é aceito na criação nem na
 *                  edição</b>: um modelo de três perguntas que se declarasse MNA
 *                  receberia as faixas da MNA sobre um total de cinco pontos.
 *                  Ele vem do seed, e o clone o preserva.
 * @param escalaNome o nome publicado do instrumento, do lado do servidor — a
 *                  tela não monta citação bibliográfica à mão. A legenda de
 *                  fonte escrita na tela já mentiu uma vez neste projeto.
 */
public record ModeloFichaResponseDto(
        UUID id,
        String nome,
        String descricao,
        Boolean ativo,
        boolean doSistema,
        String escoreCodigo,
        String escalaNome,
        String escalaReferencia,
        List<CampoFichaResponseDto> campos,
        Instant createdAt,
        Instant updatedAt
) {}
