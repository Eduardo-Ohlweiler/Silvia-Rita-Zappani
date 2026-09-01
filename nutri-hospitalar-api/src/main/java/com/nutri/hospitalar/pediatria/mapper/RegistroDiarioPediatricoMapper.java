package com.nutri.hospitalar.pediatria.mapper;

import com.nutri.hospitalar.pediatria.calculo.AcompanhamentoPediatricoCalculator;
import com.nutri.hospitalar.pediatria.calculo.EntradaAcompanhamento;
import com.nutri.hospitalar.pediatria.calculo.LinhaPercentil;
import com.nutri.hospitalar.pediatria.calculo.ResultadoAcompanhamento;
import com.nutri.hospitalar.pediatria.dtos.AcompanhamentoDerivadoDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoListaDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoResponseDto;
import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
import com.nutri.hospitalar.pediatria.entity.RegistroDiarioPediatrico;
import com.nutri.hospitalar.pediatria.enums.IndiceOms;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;

import java.time.LocalDate;
import java.time.Period;

/**
 * Converte um dia de acompanhamento pediátrico na forma que a tela conhece,
 * <b>derivando na leitura</b>.
 *
 * <p>Ao contrário de {@code AvaliacaoUtiMapper}, aqui os derivados <b>são</b>
 * calculados — e é correto: eles não são prescrição registrada, são leitura de
 * acompanhamento. A avaliação grava o que foi prescrito naquele dia e não pode
 * mudar quando a régua mudar; o acompanhamento lê o que aconteceu, e lê contra a
 * régua de hoje ({@code docs/11} §8).
 *
 * <p>A <b>linha da OMS chega de fora</b>, resolvida pelo service — o calculador é
 * puro e não vê banco, como {@code CalculoPediatricoCalculator}.
 */
public final class RegistroDiarioPediatricoMapper {

    private RegistroDiarioPediatricoMapper() {}

    /**
     * A idade <b>no dia do registro</b>, da data de nascimento.
     *
     * <p>Nunca copiada da avaliação: na pediatria a idade é o eixo X das curvas,
     * e 30 dias de internação de um lactente atravessam uma linha inteira da
     * tabela da OMS. Idade copiada envelheceria calada ({@code docs/11} §3).
     *
     * <p>Público porque o service precisa dela <b>antes</b> de buscar a linha de
     * percentil — é a idade que diz qual linha buscar.
     */
    public static Integer idadeNoDia(Pessoa paciente, LocalDate data) {
        if (paciente.getDataNascimento() == null || data == null) return null;
        return (int) Period.between(paciente.getDataNascimento(), data).toTotalMonths();
    }

    /** As entradas que o calculador recebe, já reunidas com as metas da avaliação. */
    public static EntradaAcompanhamento toEntrada(RegistroDiarioPediatrico r) {
        AvaliacaoPediatrica a = r.getAvaliacao();
        Pessoa p = r.getPessoa();

        return new EntradaAcompanhamento(
                sexoDoPaciente(p, a),
                idadeNoDia(p, r.getData()),

                r.getPesoKg(),
                r.getEstaturaCm(),

                r.getVolPrescrito24h(),
                r.getVolRecebido24h(),
                r.getTomadasPrevistas(),
                r.getTomadasAceitas(),

                a == null ? null : a.getVolumeTotal(),
                a == null ? null : a.getVet(),
                a == null ? null : a.getProteinaNecessidade(),
                // O RETRATO gravado na avaliação, não o catálogo de hoje: a
                // oferta daquele dia foi calculada com aquela composição.
                a == null ? null : a.getFormulaKcalPor100ml(),
                a == null ? null : a.getFormulaProteinaPor100ml());
    }

    public static RegistroDiarioPediatricoResponseDto toResponse(RegistroDiarioPediatrico r,
                                                                 LinhaPercentil linha) {
        AvaliacaoPediatrica a = r.getAvaliacao();
        ResultadoAcompanhamento d = AcompanhamentoPediatricoCalculator.calcular(
                toEntrada(r), linha);

        return new RegistroDiarioPediatricoResponseDto(
                r.getId(),
                r.getPessoa().getId(),
                r.getPessoa().getNome(),

                a == null ? null : a.getId(),
                a == null ? null : a.getDataAvaliacao(),
                a == null ? null : a.getVolumeTotal(),
                a == null ? null : a.getVet(),
                a == null ? null : a.getProteinaNecessidade(),
                a == null ? null : a.getFormulaNome(),

                r.getData(),

                r.getPesoKg(), r.getEstaturaCm(),
                r.getVolPrescrito24h(), r.getVolRecebido24h(),
                r.getTomadasPrevistas(), r.getTomadasAceitas(),
                r.getObservacao(),

                toDerivado(d),

                r.getCreatedAt(),
                r.getUpdatedAt());
    }

    public static RegistroDiarioPediatricoListaDto toLista(RegistroDiarioPediatrico r,
                                                           LinhaPercentil linha) {
        ResultadoAcompanhamento d = AcompanhamentoPediatricoCalculator.calcular(
                toEntrada(r), linha);

        return new RegistroDiarioPediatricoListaDto(
                r.getId(),
                r.getPessoa().getNome(),
                r.getData(),
                d.idadeMeses(),
                r.getPesoKg(),
                r.getVolPrescrito24h(),
                r.getVolRecebido24h(),
                d.percentualRecebido(),
                d.adequacaoCalorica(),
                d.aceitacaoTomadas(),
                r.getAvaliacao() != null);
    }

    public static AcompanhamentoDerivadoDto toDerivado(ResultadoAcompanhamento d) {
        return new AcompanhamentoDerivadoDto(
                d.idadeMeses(), d.motivoIdade(),
                d.imc(), d.motivoImc(),

                ResultadoPediatricoMapper.classificacao(IndiceOms.PESO_IDADE, d.pesoIdade()),
                ResultadoPediatricoMapper.classificacao(IndiceOms.ESTATURA_IDADE, d.estaturaIdade()),
                ResultadoPediatricoMapper.classificacao(IndiceOms.IMC_IDADE, d.imcIdade()),
                d.motivoEstadoNutricional(),

                d.percentualRecebido(), d.referenciaDoRecebido(), d.motivoPercentualRecebido(),
                d.caloriasRecebidas(), d.proteinaRecebida(), d.motivoOferta(),
                d.caloriasPorKg(), d.proteinaPorKg(), d.motivoPorQuilo(),
                d.adequacaoCalorica(), d.motivoAdequacaoCalorica(),
                d.adequacaoProteica(), d.motivoAdequacaoProteica(),
                d.aceitacaoTomadas(), d.motivoAceitacao());
    }

    /**
     * O sexo do paciente, com a avaliação como reserva.
     *
     * <p>A {@link Pessoa} é a fonte — é dela que o cadastro responde. Mas o sexo
     * é opcional lá, e a avaliação o exige: quando o cadastro está incompleto e
     * existe avaliação vinculada, usar o dela é melhor que não classificar. Sem
     * nenhum dos dois, o calculador diz o que falta.
     */
    private static Sexo sexoDoPaciente(Pessoa p, AvaliacaoPediatrica a) {
        if (p.getSexo() != null) return p.getSexo();
        return a == null ? null : a.getSexo();
    }
}
