package com.nutri.hospitalar.pediatria.dtos;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Linha da listagem. Enxuta de propósito: a grade mostra seis colunas e não
 * precisa carregar os doze resultados nem o retrato da fórmula.
 *
 * <p>A faixa do IMC vem junto do rótulo para a grade colorir o badge sem
 * interpretar texto.
 */
public record AvaliacaoPediatricaListaDto(
        UUID id,
        LocalDate dataAvaliacao,
        String pacienteNome,
        Integer idadeMeses,
        BigDecimal peso,
        BigDecimal imc,
        FaixaOms classifImcIdade,
        String classifImcIdadeRotulo,
        String formulaNome
) {}
