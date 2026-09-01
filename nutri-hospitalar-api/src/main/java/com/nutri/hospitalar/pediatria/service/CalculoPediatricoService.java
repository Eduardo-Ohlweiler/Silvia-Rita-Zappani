package com.nutri.hospitalar.pediatria.service;

import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator;
import com.nutri.hospitalar.pediatria.calculo.EntradaPediatrica;
import com.nutri.hospitalar.pediatria.calculo.LinhaPercentil;
import com.nutri.hospitalar.pediatria.calculo.ResultadoPediatrico;
import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import com.nutri.hospitalar.pediatria.entity.PercentilOms;
import com.nutri.hospitalar.pediatria.repository.PercentilOmsRepository;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolve o que o calculador precisa — a linha da curva da OMS e a composição
 * da fórmula — e chama o cálculo.
 *
 * <p>É o único caminho de cálculo do sistema. O {@code POST /calcular} da tela
 * e a gravação da avaliação passam os dois por aqui, então é impossível a tela
 * mostrar um número e o banco guardar outro.
 *
 * <p>O calculador em si não conhece Spring nem banco: este service é a casca
 * que busca os dados e a classe pura faz a conta.
 */
@Service
@RequiredArgsConstructor
public class CalculoPediatricoService {

    /** Faixa das curvas de crescimento da OMS, em meses. Ver docs/09 §4.2. */
    private static final int IDADE_MAXIMA_CURVAS = 60;

    private final PercentilOmsRepository percentilOmsRepository;

    /**
     * @param formula a fórmula escolhida, ou {@code null} — sem ela o bloco da
     *                dieta vem ausente com o motivo, não com erro
     */
    @Transactional(readOnly = true)
    public ResultadoPediatrico calcular(Sexo sexo,
                                        Integer idadeMeses,
                                        BigDecimal peso,
                                        BigDecimal estatura,
                                        FormulaLactea formula,
                                        BigDecimal volumeMl,
                                        BigDecimal frequenciaHoras) {

        return calcular(sexo, idadeMeses, peso, estatura,
                formula != null ? formula.getKcalPor100ml() : null,
                formula != null ? formula.getProteinaPor100ml() : null,
                volumeMl, frequenciaHoras);
    }

    /**
     * O mesmo cálculo, recebendo a <b>composição</b> em vez da fórmula.
     *
     * <p>Existe para a avaliação salva, que guarda o retrato da fórmula
     * ({@code formula_kcal_por_100ml}, {@code formula_proteina_por_100ml}) e
     * não pode depender de o catálogo continuar igual — a fórmula pode ter
     * mudado, ou saído do catálogo, desde que a dieta foi prescrita.
     */
    @Transactional(readOnly = true)
    public ResultadoPediatrico calcular(Sexo sexo,
                                        Integer idadeMeses,
                                        BigDecimal peso,
                                        BigDecimal estatura,
                                        BigDecimal kcalPor100ml,
                                        BigDecimal proteinaPor100ml,
                                        BigDecimal volumeMl,
                                        BigDecimal frequenciaHoras) {

        EntradaPediatrica entrada = new EntradaPediatrica(
                sexo, idadeMeses, peso, estatura,
                kcalPor100ml, proteinaPor100ml, volumeMl, frequenciaHoras);

        return CalculoPediatricoCalculator.calcular(entrada, buscarLinha(sexo, idadeMeses));
    }

    /**
     * Refaz o cálculo de uma avaliação <b>gravada</b>, para colher dele só os
     * <b>motivos de ausência</b>.
     *
     * <p>Nenhum número deste resultado chega à tela: quem monta a resposta é
     * {@link com.nutri.hospitalar.pediatria.mapper.ResultadoPediatricoMapper},
     * e ele lê todo valor das colunas da avaliação. Sem isto, uma avaliação
     * fora da faixa das DRIs reabre com traço mudo — o silêncio que este
     * módulo existe para não repetir.
     *
     * <p>Usa o <b>retrato</b> da fórmula, não o catálogo de hoje: o motivo tem
     * de explicar a avaliação como ela foi feita.
     */
    @Transactional(readOnly = true)
    public ResultadoPediatrico motivosDe(AvaliacaoPediatrica a) {
        return calcular(
                a.getSexo(), a.getIdadeMeses(), a.getPeso(), a.getEstatura(),
                a.getFormulaKcalPor100ml(), a.getFormulaProteinaPor100ml(),
                a.getVolumeMl(), a.getFrequenciaHoras());
    }

    /**
     * A linha da curva para o sexo e a idade, ou {@code null} quando não há.
     *
     * <p>Busca por igualdade exata de mês inteiro, como a planilha. Fora de 0 a
     * 60 meses nem consulta o banco — não existe linha, e o calculador explica
     * a ausência.
     */
    private LinhaPercentil buscarLinha(Sexo sexo, Integer idadeMeses) {
        if (sexo == null || idadeMeses == null
                || idadeMeses < 0 || idadeMeses > IDADE_MAXIMA_CURVAS) {
            return null;
        }

        return percentilOmsRepository.findBySexoAndIdadeMeses(sexo, idadeMeses)
                .map(CalculoPediatricoService::toLinha)
                .orElse(null);
    }

    /**
     * As linhas da curva de um sexo inteiro, indexadas pela idade em meses.
     *
     * <p>Existe para o <b>acompanhamento diário</b>, que classifica muitos dias
     * de uma vez: buscar linha por linha faria uma consulta por registro — 90
     * consultas num painel de três meses, o clássico N+1. A tabela toda tem 61
     * linhas por sexo, então trazê-la inteira sai mais barato que a metade das
     * idas ao banco.
     *
     * <p>Idades fora de 0 a 60 meses simplesmente não estão no mapa, e um
     * {@code get} devolve {@code null} — que é exatamente o que o calculador
     * espera para explicar a ausência.
     */
    @Transactional(readOnly = true)
    public Map<Integer, LinhaPercentil> linhasPorIdade(Sexo sexo) {
        if (sexo == null) return Map.of();

        return percentilOmsRepository
                .findBySexoAndIdadeMesesBetweenOrderByIdadeMesesAsc(sexo, 0, IDADE_MAXIMA_CURVAS)
                .stream()
                .collect(Collectors.toMap(PercentilOms::getIdadeMeses,
                                          CalculoPediatricoService::toLinha));
    }

    private static LinhaPercentil toLinha(PercentilOms p) {
        return new LinhaPercentil(
                p.getPesoP15(), p.getPesoP85(),
                p.getEstaturaP15(), p.getEstaturaP85(),
                p.getImcP15(), p.getImcP85());
    }
}
