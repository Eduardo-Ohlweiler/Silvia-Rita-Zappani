package com.nutri.hospitalar.pediatria.service;

import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator;
import com.nutri.hospitalar.pediatria.calculo.EntradaPediatrica;
import com.nutri.hospitalar.pediatria.calculo.LinhaPercentil;
import com.nutri.hospitalar.pediatria.calculo.ResultadoPediatrico;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import com.nutri.hospitalar.pediatria.entity.PercentilOms;
import com.nutri.hospitalar.pediatria.repository.PercentilOmsRepository;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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

        EntradaPediatrica entrada = new EntradaPediatrica(
                sexo, idadeMeses, peso, estatura,
                formula != null ? formula.getKcalPor100ml() : null,
                formula != null ? formula.getProteinaPor100ml() : null,
                volumeMl, frequenciaHoras);

        return CalculoPediatricoCalculator.calcular(entrada, buscarLinha(sexo, idadeMeses));
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

    private static LinhaPercentil toLinha(PercentilOms p) {
        return new LinhaPercentil(
                p.getPesoP15(), p.getPesoP85(),
                p.getEstaturaP15(), p.getEstaturaP85(),
                p.getImcP15(), p.getImcP85());
    }
}
