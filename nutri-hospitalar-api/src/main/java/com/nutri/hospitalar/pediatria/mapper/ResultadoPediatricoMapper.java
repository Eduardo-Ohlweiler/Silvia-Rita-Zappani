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
 * #toResponse(ResultadoPediatrico)} vem do cálculo recém-feito; {@link
 * #toResponse(AvaliacaoPediatrica, ResultadoPediatrico)} vem do banco e traz os
 * <b>números gravados</b>. Os dois trazem o motivo de ausência.
 * Ver {@link ResultadoPediatricoDto}.
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
     * Avaliação salva: <b>os números do banco, os motivos do recálculo</b>.
     *
     * <p>Todo número desta resposta vem das colunas gravadas — nenhum é
     * recalculado, que é o ponto de um registro clínico. Do parâmetro {@code
     * motivos} sai <b>apenas texto</b>: as frases que explicam por que um campo
     * está vazio.
     *
     * <p>Antes os motivos vinham nulos, e a avaliação salva mostrava traço mudo:
     * uma criança de 37 meses reabria com VET, proteína e as duas adequações em
     * branco, sem uma palavra — exatamente o que a planilha e o eroERP fazem, e
     * exatamente o que {@link com.nutri.hospitalar.pediatria.calculo.ResultadoPediatrico}
     * existe para não fazer. O motivo é função só das <b>entradas</b>, que estão
     * gravadas na própria avaliação, então recalculá-lo não move número nenhum.
     *
     * @param a       a avaliação gravada — a fonte de todos os números
     * @param motivos cálculo refeito sobre as entradas gravadas, do qual se
     *                aproveitam só os motivos de ausência
     */
    public static ResultadoPediatricoDto toResponse(AvaliacaoPediatrica a,
                                                    ResultadoPediatrico motivos) {
        return new ResultadoPediatricoDto(
                new EstadoNutricionalDto(
                        a.getImc(),
                        motivos.motivoImc(),
                        classificacao(IndiceOms.PESO_IDADE, a.getClassifPesoIdade()),
                        classificacao(IndiceOms.ESTATURA_IDADE, a.getClassifEstaturaIdade()),
                        classificacao(IndiceOms.IMC_IDADE, a.getClassifImcIdade()),
                        motivos.motivoEstadoNutricional()),
                new NecessidadesDto(
                        a.getVet(), motivos.motivoVet(),
                        a.getProteinaNecessidade(), motivos.motivoProteina()),
                new DietaCalculadaDto(
                        a.getVezesDia(), a.getVolumeTotal(),
                        a.getCaloriasTotais(), a.getProteinaTotal(),
                        a.getPercCalorico(), a.getPercProteico(),
                        motivos.motivoDieta()));
    }

    /** Faixa para colorir, rótulo para escrever. Ver {@link ClassificacaoDto}. */
    static ClassificacaoDto classificacao(IndiceOms indice, FaixaOms faixa) {
        return faixa == null ? null : new ClassificacaoDto(faixa, indice.rotulo(faixa));
    }
}
