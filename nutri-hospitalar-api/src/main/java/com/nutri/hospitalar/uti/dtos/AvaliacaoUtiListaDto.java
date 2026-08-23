package com.nutri.hospitalar.uti.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha da lista de avaliações.
 *
 * <p>Traz o mínimo que permite decidir qual abrir: quem, quando, o estado
 * nutricional e o que estava prescrito. Carregar a avaliação inteira para
 * desenhar uma grade seria transferir setenta colunas por linha.
 *
 * @param classificacaoImc o rótulo gravado, não recalculado
 * @param tomClassificacao a cor que a linha usa — atribuída pelo servidor no dia
 *                         da avaliação, como todo o resto do registro
 */
public record AvaliacaoUtiListaDto(
        UUID id,
        String pacienteNome,
        String profissionalNome,
        LocalDate dataAvaliacao,
        BigDecimal pesoTrabalhoKg,
        String pesoTrabalhoOrigem,
        BigDecimal imc,
        String classificacaoImc,
        String tomClassificacao,
        BigDecimal metaEnergetica,
        String formulaNome
) {}
