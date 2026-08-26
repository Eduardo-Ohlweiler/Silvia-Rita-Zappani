package com.nutri.hospitalar.uti.service;

import com.nutri.hospitalar.exceptions.BusinessException;
import com.nutri.hospitalar.uti.calculo.AntropometriaCalculator;
import com.nutri.hospitalar.uti.calculo.AvaliacaoUtiCalculator;
import com.nutri.hospitalar.uti.calculo.EntradaUti;
import com.nutri.hospitalar.uti.calculo.FormulaEnteralResolvida;
import com.nutri.hospitalar.uti.calculo.ResultadoUti;
import com.nutri.hospitalar.uti.dtos.CalculoUtiRequestDto;
import com.nutri.hospitalar.uti.entity.FormulaEnteral;
import com.nutri.hospitalar.uti.repository.PercentilCbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Resolve o que depende do banco e chama o cálculo.
 *
 * <p>Duas buscas e nada mais: a fórmula enteral no catálogo (respeitando o
 * tenant, e enxergando as globais) e o P50 da circunferência do braço na tabela
 * de referência. O resto é
 * {@link AvaliacaoUtiCalculator}, que é classe pura.
 *
 * <p><b>Nada é gravado aqui.</b> Este é o endpoint que a tela chama a cada
 * alteração de campo; a persistência é da avaliação, que vem na fatia seguinte.
 *
 * <p><b>Nada é registrado em log, tampouco.</b> Peso, diagnóstico e prescrição
 * são dado de saúde sob a LGPD — regra 5 do {@code CLAUDE.md}. Este service não
 * loga entrada nem resultado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculoUtiService {

    private final FormulaEnteralService formulaEnteralService;
    private final PercentilCbRepository percentilCbRepository;

    @Transactional(readOnly = true)
    public ResultadoUti calcular(CalculoUtiRequestDto dto) {
        EntradaUti entrada = paraEntrada(dto);

        recusarAmputacaoSobreposta(entrada);

        return AvaliacaoUtiCalculator.calcular(
                entrada, resolverFormula(dto), buscarP50(dto));
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Segmentos amputados que se contêm devolvem <b>422</b>, nomeando os dois.
     *
     * <p>Escolher "Membro superior" e "Mão" desconta a mão duas vezes. Devolver
     * um peso menor que o real em silêncio seria o pior desfecho: o número sai
     * plausível e vira prescrição.
     *
     * <p>É 422 e não 400 porque o corpo está bem formado — o que não fecha é a
     * combinação clínica.
     */
    private void recusarAmputacaoSobreposta(EntradaUti entrada) {
        AntropometriaCalculator.conflitoDeSegmentos(entrada.segmentosOuVazio())
                .ifPresent(mensagem -> { throw new BusinessException(mensagem); });
    }

    /**
     * A fórmula do catálogo, já dentro do tenant.
     *
     * <p>Usa {@code buscarVisivel}, que enxerga a do cliente e a global — e
     * devolve 404 para a de outro cliente.
     */
    private FormulaEnteralResolvida resolverFormula(CalculoUtiRequestDto dto) {
        if (dto.formulaEnteralId() == null) return null;

        FormulaEnteral f = formulaEnteralService.buscarVisivel(dto.formulaEnteralId());

        return new FormulaEnteralResolvida(
                f.getNome(), f.getDensidadeKcalMl(), f.getProteinaGL(),
                f.getChoGL(), f.getLipGL(), f.getFibrasGL(),
                f.getPotassioMgL(), f.getAguaLivrePerc());
    }

    /**
     * O P50 da faixa etária — <b>buscado</b>, não digitado.
     *
     * <p>Na planilha ele é digitado à mão em {@code Estimativas!J24} com a
     * tabela ao lado sem ser consultada (defeito 19), e o exemplo em cache usa a
     * linha errada para a idade. Fora de 18 a 90,9 anos não há linha, e o
     * resultado sai como ausência com motivo — o eroERP extrapola em silêncio
     * usando a linha do extremo.
     */
    private BigDecimal buscarP50(CalculoUtiRequestDto dto) {
        if (dto.sexo() == null || dto.idadeAnos() == null) return null;

        return percentilCbRepository
                .findFaixaDe(dto.sexo(), new BigDecimal(dto.idadeAnos()))
                .map(p -> p.getP50Cm())
                .orElse(null);
    }

    private EntradaUti paraEntrada(CalculoUtiRequestDto d) {
        return new EntradaUti(
                d.sexo(), d.etnia(), d.idadeAnos(),
                d.alturaCm(), d.alturaJoelhoCm(),
                d.circBracoCm(), d.circPanturrilhaCm(), d.circAbdominalCm(),
                d.pesoAtualKg(), d.pesoUsualKg(),
                d.janelaPerda(), d.segmentosAmputados(),
                d.populacaoReferencia(), d.origemPesoPreferida(),
                d.fase(), d.terapiaRenal(), d.kcalPorKgAlvo(), d.proteinaPorKgAlvo(),
                d.posicaoNaFaixa(),
                d.modoInfusao(), d.volumePorTempo(), d.tempo(),
                d.volumeDietaManualMl());
    }
}
