package com.nutri.hospitalar.clinica.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A linha da listagem.
 *
 * <p>{@code modeloNome} vem do <b>retrato</b> da ficha, não do modelo: a lista
 * tem de continuar dizendo de qual modelo cada ficha veio mesmo depois de o
 * modelo ser apagado ou renomeado.
 *
 * @param respondidas quantas perguntas têm resposta, de {@code totalPerguntas}.
 *                    É o que distingue a ficha completa da que ficou pela
 *                    metade, sem precisar abrir.
 * @param escoreTotal  o número congelado no dia, nulo quando a escala não pôde
 *                    ser concluída. Vem da coluna plana e não do JSON, porque
 *                    JSON não se ordena nem se filtra na native query.
 * @param escoreClassificacao o rótulo, e {@code escoreTom} a cor — atribuída
 *                    pelo servidor, para a lista não colorir por
 *                    {@code texto.contains("adequado")}, que foi como o eroERP
 *                    quebrou ao mudar uma palavra.
 */
public record FichaAnamneseListaDto(
        UUID id,
        UUID pacienteId,
        String pacienteNome,
        String profissionalNome,
        LocalDate dataPreenchimento,
        String modeloNome,
        int respondidas,
        int totalPerguntas,
        BigDecimal escoreTotal,
        String escoreClassificacao,
        String escoreTom
) {}
