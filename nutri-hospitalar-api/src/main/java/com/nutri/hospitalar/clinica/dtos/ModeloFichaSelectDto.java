package com.nutri.hospitalar.clinica.dtos;

import java.util.UUID;

/**
 * @param doSistema vai junto para o combo poder dizer de onde o modelo vem. Quem
 *                  escolhe precisa distinguir o modelo do sistema do que a
 *                  própria clínica montou.
 */
public record ModeloFichaSelectDto(
        UUID id,
        String nome,
        String descricao,
        boolean doSistema,
        int totalCampos
) {}
