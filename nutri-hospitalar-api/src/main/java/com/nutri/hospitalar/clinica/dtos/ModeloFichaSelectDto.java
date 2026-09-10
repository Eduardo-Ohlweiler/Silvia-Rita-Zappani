package com.nutri.hospitalar.clinica.dtos;

import java.util.UUID;

/**
 * @param doSistema vai junto para o combo poder dizer de onde o modelo vem. Quem
 *                  escolhe precisa distinguir o modelo do sistema do que a
 *                  própria clínica montou.
 * @param escoreCodigo para o combo poder marcar o modelo que calcula escore, e
 *                  para a tela de preenchimento saber que vai desenhar o painel.
 */
public record ModeloFichaSelectDto(
        UUID id,
        String nome,
        String descricao,
        boolean doSistema,
        String escoreCodigo,
        int totalCampos
) {}
