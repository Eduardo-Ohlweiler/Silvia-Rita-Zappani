package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.calculo.ResultadoUti;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A avaliação gravada, pronta para reabrir o formulário.
 *
 * <p><b>Devolve as entradas e os resultados separados, e os dois vêm do
 * banco.</b> As entradas repovoam os campos; os resultados vão direto para a
 * tela <b>sem recalcular</b>. Se as equações mudarem amanhã, este registro
 * continua mostrando o que se decidiu no dia — prontuário não se reescreve
 * sozinho.
 *
 * <p>O {@code resultado} é reconstruído das colunas gravadas e tem exatamente a
 * forma de {@link ResultadoUti}, o mesmo tipo que {@code /uti/calculo} devolve.
 * Assim a tela usa um tipo só, e o componente de cálculo não precisa saber se
 * está exibindo um cálculo ao vivo ou um registro de três meses atrás.
 *
 * @param pacienteNome     desnormalizado para a tela não precisar de outra ida
 * @param formulaRemovida  a fórmula saiu do catálogo depois da avaliação. O
 *                         retrato dela continua no resultado — este sinalizador
 *                         só existe para a tela poder dizer isso por escrito.
 */
public record AvaliacaoUtiResponseDto(
        UUID id,
        UUID pacienteId,
        String pacienteNome,
        UUID profissionalId,
        String profissionalNome,
        LocalDate dataAvaliacao,
        CalculoUtiRequestDto calculo,
        ResultadoUti resultado,
        boolean formulaRemovida,
        String observacao,
        Instant createdAt,
        Instant updatedAt
) {}
