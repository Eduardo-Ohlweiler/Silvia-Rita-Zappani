package com.nutri.hospitalar.pediatria.mapper;

import com.nutri.hospitalar.pediatria.calculo.ResultadoPediatrico;
import com.nutri.hospitalar.pediatria.dtos.ClassificacaoDto;
import com.nutri.hospitalar.pediatria.dtos.DietaCalculadaDto;
import com.nutri.hospitalar.pediatria.dtos.EstadoNutricionalDto;
import com.nutri.hospitalar.pediatria.dtos.NecessidadesDto;
import com.nutri.hospitalar.pediatria.dtos.ResultadoPediatricoDto;
import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
import com.nutri.hospitalar.pediatria.enums.FaixaOms;
import com.nutri.hospitalar.pediatria.enums.IndiceOms;

/**
 * Leva o resultado ao formato que a tela consome — três blocos, um por aba.
 *
 * <p>Há dois caminhos de entrada e eles não são iguais: {@link
 * #toResponse(ResultadoPediatrico)} vem do cálculo recém-feito e traz os
 * motivos de ausência; {@link #toResponse(AvaliacaoPediatrica)} vem do banco e
 * traz só os números gravados, sem motivo. Ver {@link ResultadoPediatricoDto}.
 */
public final class ResultadoPediatricoMapper {

    private ResultadoPediatricoMapper() {}

    /** Cálculo recém-feito: números e motivos. */
    public static ResultadoPediatricoDto toResponse(ResultadoPediatrico r) {
        return new ResultadoPediatricoDto(
                new EstadoNutricionalDto(
                        r.imc(),
                        r.motivoImc(),
                        classificacao(IndiceOms.PESO_IDADE, r.pesoIdade()),
                        classificacao(IndiceOms.ESTATURA_IDADE, r.estaturaIdade()),
                        classificacao(IndiceOms.IMC_IDADE, r.imcIdade()),
                        r.motivoEstadoNutricional()),
                new NecessidadesDto(
                        r.vet(), r.motivoVet(),
                        r.proteinaNecessidade(), r.motivoProteina()),
                new DietaCalculadaDto(
                        r.vezesDia(), r.volumeTotal(),
                        r.caloriasTotais(), r.proteinaTotal(),
                        r.percCalorico(), r.percProteico(),
                        r.motivoDieta()));
    }

    /**
     * Avaliação salva: só o que foi gravado.
     *
     * <p>Nada é recalculado aqui — é o ponto do registro. Os motivos vêm nulos
     * porque explicam o instante da digitação, e voltam assim que o usuário
     * mexe numa entrada e a tela chama o {@code /calcular}.
     */
    public static ResultadoPediatricoDto toResponse(AvaliacaoPediatrica a) {
        return new ResultadoPediatricoDto(
                new EstadoNutricionalDto(
                        a.getImc(),
                        null,
                        classificacao(IndiceOms.PESO_IDADE, a.getClassifPesoIdade()),
                        classificacao(IndiceOms.ESTATURA_IDADE, a.getClassifEstaturaIdade()),
                        classificacao(IndiceOms.IMC_IDADE, a.getClassifImcIdade()),
                        null),
                new NecessidadesDto(
                        a.getVet(), null,
                        a.getProteinaNecessidade(), null),
                new DietaCalculadaDto(
                        a.getVezesDia(), a.getVolumeTotal(),
                        a.getCaloriasTotais(), a.getProteinaTotal(),
                        a.getPercCalorico(), a.getPercProteico(),
                        null));
    }

    /** Faixa para colorir, rótulo para escrever. Ver {@link ClassificacaoDto}. */
    static ClassificacaoDto classificacao(IndiceOms indice, FaixaOms faixa) {
        return faixa == null ? null : new ClassificacaoDto(faixa, indice.rotulo(faixa));
    }
}
