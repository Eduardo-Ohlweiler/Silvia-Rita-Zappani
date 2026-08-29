package com.nutri.hospitalar.pediatria.mapper;

import com.nutri.hospitalar.pediatria.calculo.ResultadoPediatrico;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaListaDto;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaResponseDto;
import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import com.nutri.hospitalar.pediatria.enums.IndiceOms;
import com.nutri.hospitalar.pessoa.entity.Pessoa;

import java.util.UUID;

public final class AvaliacaoPediatricaMapper {

    private AvaliacaoPediatricaMapper() {}

    /**
     * @param motivos cálculo sobre as entradas gravadas, do qual só os motivos
     *                de ausência são aproveitados — todo número sai de {@code a}.
     *                Ver {@link ResultadoPediatricoMapper#toResponse(AvaliacaoPediatrica, ResultadoPediatrico)}
     */
    public static AvaliacaoPediatricaResponseDto toResponse(AvaliacaoPediatrica a,
                                                            ResultadoPediatrico motivos) {
        return new AvaliacaoPediatricaResponseDto(
                a.getId(),

                a.getPaciente().getId(),
                a.getPaciente().getNome(),
                id(a.getProfissional()),
                nome(a.getProfissional()),

                a.getDataAvaliacao(),
                a.getSexo(),
                a.getIdadeMeses(),
                a.getPeso(),
                a.getEstatura(),

                idFormula(a.getFormulaLactea()),
                // Retrato, não leitura do catálogo: são os números com que a
                // dieta foi prescrita, ainda que a fórmula tenha mudado depois.
                a.getFormulaNome(),
                a.getFormulaKcalPor100ml(),
                a.getFormulaProteinaPor100ml(),
                a.getVolumeMl(),
                a.getFrequenciaHoras(),

                ResultadoPediatricoMapper.toResponse(a, motivos),

                a.getObservacao(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    public static AvaliacaoPediatricaListaDto toLista(AvaliacaoPediatrica a) {
        return new AvaliacaoPediatricaListaDto(
                a.getId(),
                a.getDataAvaliacao(),
                a.getPaciente().getNome(),
                a.getIdadeMeses(),
                a.getPeso(),
                a.getImc(),
                a.getClassifImcIdade(),
                IndiceOms.IMC_IDADE.rotulo(a.getClassifImcIdade()),
                a.getFormulaNome());
    }

    private static UUID id(Pessoa pessoa) {
        return pessoa == null ? null : pessoa.getId();
    }

    private static String nome(Pessoa pessoa) {
        return pessoa == null ? null : pessoa.getNome();
    }

    private static UUID idFormula(FormulaLactea formula) {
        return formula == null ? null : formula.getId();
    }
}
