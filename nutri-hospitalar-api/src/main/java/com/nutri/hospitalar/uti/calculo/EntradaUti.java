package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.enums.EtniaChumlea;
import com.nutri.hospitalar.uti.enums.FaseTerapia;
import com.nutri.hospitalar.uti.enums.JanelaPerdaPeso;
import com.nutri.hospitalar.uti.enums.ModoInfusao;
import com.nutri.hospitalar.uti.enums.OrigemValor;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.enums.SegmentoAmputado;
import com.nutri.hospitalar.uti.enums.TerapiaRenal;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Tudo o que as quatro abas da tela de cálculo coletam, já normalizado pelo
 * service.
 *
 * <p><b>Nenhum campo aparece em duas abas.</b> As entradas são disjuntas; o que
 * corre entre as abas são os <i>resultados</i>, e é por isso que existe a
 * cascata tipada. Ver {@code docs/04} §7.
 *
 * <p>Todo campo é anulável: a tela recalcula a cada 500 ms, e na maior parte do
 * tempo o formulário está pela metade. O calculador devolve o que consegue e diz
 * o motivo do que não conseguiu.
 *
 * @param populacaoReferencia qual coluna de ajuste de CB e CP usar. Só tem
 *                            efeito em IMC &lt; 18,5 — ver {@code docs/10} §2.9.
 * @param origemPesoPreferida força uma fonte de peso específica. Nulo deixa a
 *                            escolha para {@link AvaliacaoUtiCalculator}, que
 *                            declara a ordem que usa.
 * @param volumeDietaManualMl volume de dieta informado à mão na aba de
 *                            hidratação, para quem calcula a água sem ter
 *                            preenchido a dieta enteral
 */
public record EntradaUti(

        // ─── Antropometria ──────────────────────────────────────────────
        Sexo sexo,
        EtniaChumlea etnia,
        Integer idadeAnos,
        BigDecimal alturaCm,
        BigDecimal alturaJoelhoCm,
        BigDecimal circBracoCm,
        BigDecimal circPanturrilhaCm,
        BigDecimal circAbdominalCm,
        BigDecimal pesoAtualKg,
        BigDecimal pesoUsualKg,
        JanelaPerdaPeso janelaPerda,
        Set<SegmentoAmputado> segmentosAmputados,
        PopulacaoReferencia populacaoReferencia,
        OrigemValor origemPesoPreferida,

        // ─── Necessidades ───────────────────────────────────────────────
        FaseTerapia fase,
        TerapiaRenal terapiaRenal,
        BigDecimal kcalPorKgAlvo,
        BigDecimal proteinaPorKgAlvo,

        // ─── Dieta enteral ──────────────────────────────────────────────
        ModoInfusao modoInfusao,
        BigDecimal volumePorTempo,
        BigDecimal tempo,

        // ─── Hidratação ─────────────────────────────────────────────────
        BigDecimal volumeDietaManualMl
) {

    /** A população clínica é o padrão: o módulo é de UTI. */
    public PopulacaoReferencia populacaoOuPadrao() {
        return populacaoReferencia == null ? PopulacaoReferencia.POPULACAO_CLINICA : populacaoReferencia;
    }

    public TerapiaRenal terapiaRenalOuNenhuma() {
        return terapiaRenal == null ? TerapiaRenal.NENHUMA : terapiaRenal;
    }

    public Set<SegmentoAmputado> segmentosOuVazio() {
        return segmentosAmputados == null ? Set.of() : segmentosAmputados;
    }
}
