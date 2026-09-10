package com.nutri.hospitalar.clinica.dtos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A ficha aberta, inteira.
 *
 * @param modeloId       rastro. <b>Nulo quando o modelo foi apagado</b> — e a
 *                       ficha continua legível, porque o que a explica é o
 *                       {@code modeloNome} e o retrato de cada resposta.
 * @param modeloNome     o retrato. É o que a tela e o papel mostram.
 * @param modeloRemovido o modelo foi apagado do catálogo. A ficha continua
 *                       inteira — é para isso que o retrato existe.
 * @param modeloAlterado o modelo ainda existe, mas <b>não é mais o que gerou
 *                       esta ficha</b>: alguma pergunta mudou de texto, de tipo,
 *                       de opções, ou entrou e saiu. A tela mostra o aviso e
 *                       desenha pelo retrato. Fórmula alterada engana mais que
 *                       fórmula removida, e pergunta alterada engana mais ainda,
 *                       porque não há número ao lado para não fechar.
 * @param escore         o escore <b>congelado no dia</b>, desserializado de
 *                       {@code escore_json}. Nulo quando o modelo não aplica
 *                       escala. Ele não é recalculado ao abrir: corrigir a data
 *                       de nascimento de um paciente mudaria, calado, o escore
 *                       NRS de toda ficha antiga dele. Ver docs/13 §5.
 */
public record FichaAnamneseResponseDto(
        UUID id,
        UUID pacienteId,
        String pacienteNome,
        UUID profissionalId,
        String profissionalNome,
        LocalDate dataPreenchimento,
        UUID modeloId,
        String modeloNome,
        boolean modeloRemovido,
        boolean modeloAlterado,
        EscoreDto escore,
        List<RespostaFichaResponseDto> respostas,
        String observacao,
        Instant createdAt,
        Instant updatedAt
) {}
