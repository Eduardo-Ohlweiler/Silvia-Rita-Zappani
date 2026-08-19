package com.nutri.hospitalar.pediatria.dtos;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;
import com.nutri.hospitalar.pessoa.enums.Sexo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O acompanhamento de uma criança: onde ela está hoje e por onde andou.
 *
 * <p>O sexo vem de {@code pessoa}, não da última avaliação. No eroERP vinha da
 * avaliação porque {@code Pessoa} não tinha o campo — uma criança sem avaliação
 * nenhuma ficava sem curva para comparar.
 *
 * @param idadeMesesAtual idade hoje, calculada da data de nascimento. Nula
 *                        quando o cadastro não a tem — e aí a tela mostra a
 *                        idade da última avaliação, que é o que se sabe.
 */
public record PainelPacienteDto(
        UUID pacienteId,
        String pacienteNome,
        Sexo sexo,
        LocalDate dataNascimento,
        Integer idadeMesesAtual,

        long totalAvaliacoes,
        LocalDate primeiraAvaliacao,
        LocalDate ultimaAvaliacao,

        /** A avaliação mais recente, inteira — mesma forma que a tela já lê. */
        AvaliacaoPediatricaResponseDto ultima,

        /** Ordenada por idade: é o eixo X das curvas de crescimento. */
        List<PontoEvolutivoDto> evolucao,

        List<HistoricoFormulaDto> historicoFormulas
) {

    /** Um ponto da trajetória da criança. */
    public record PontoEvolutivoDto(
            LocalDate dataAvaliacao,
            Integer idadeMeses,
            BigDecimal peso,
            BigDecimal estatura,
            BigDecimal imc,
            FaixaOms classifPesoIdade,
            FaixaOms classifEstaturaIdade,
            FaixaOms classifImcIdade,
            BigDecimal vet,
            BigDecimal proteinaNecessidade,
            BigDecimal caloriasTotais,
            BigDecimal proteinaTotal,
            BigDecimal percCalorico,
            BigDecimal percProteico
    ) {}

    /**
     * Por qual fórmula a criança passou e quando.
     *
     * <p>Sai do retrato gravado na avaliação, não do catálogo: a fórmula pode
     * ter sido desativada, e o histórico continua contando a verdade.
     */
    public record HistoricoFormulaDto(
            String formulaNome,
            long avaliacoes,
            LocalDate primeiroUso,
            LocalDate ultimoUso
    ) {}
}
