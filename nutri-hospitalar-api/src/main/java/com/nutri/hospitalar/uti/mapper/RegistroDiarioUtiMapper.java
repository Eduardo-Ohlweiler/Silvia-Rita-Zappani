package com.nutri.hospitalar.uti.mapper;

import com.nutri.hospitalar.uti.calculo.AcompanhamentoCalculator;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiListaDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiResponseDto;
import com.nutri.hospitalar.uti.entity.AvaliacaoUti;
import com.nutri.hospitalar.uti.entity.RegistroDiarioUti;

import java.math.BigDecimal;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.arredondar;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.arredondarPercentual;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.dividir;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * Monta o dia de acompanhamento com os seus derivados.
 *
 * <p>Os derivados são calculados <b>aqui, na leitura</b> — não são colunas. É a
 * diferença entre este módulo e o eroERP, onde o {@code % recebido} é campo
 * digitável ao lado de um calculado e os dois vão para o banco.
 */
public final class RegistroDiarioUtiMapper {

    private RegistroDiarioUtiMapper() {}

    private static final String SEM_AVALIACAO =
            "Sem avaliação vinculada: kcal/kg, proteína e diurese por quilo precisam do peso e da fórmula prescritos.";

    private static final String PRESCRITO_DA_AVALIACAO = "prescrito na avaliação";
    private static final String PRESCRITO_DO_DIA = "prescrito informado no dia";

    public static RegistroDiarioUtiResponseDto toResponse(RegistroDiarioUti r) {
        AvaliacaoUti a = r.getAvaliacao();

        BigDecimal volumeDaAvaliacao = a == null ? null : a.getVolumeTotalMl();
        BigDecimal peso = a == null ? null : a.getPesoTrabalhoKg();
        BigDecimal densidade = a == null ? null : a.getFormulaDensidadeKcalMl();
        BigDecimal proteinaGL = a == null ? null : a.getFormulaProteinaGL();

        /*
         * O número e a procedência saem da MESMA decisão.
         *
         * Antes, o percentual escolhia o denominador lá dentro e este mapper
         * refazia a escolha com um `positivo()` próprio, só para nomear a
         * procedência — duas cópias da regra que nada obrigava a concordar. E o
         * número escolhido não ia para lugar nenhum, então a tela exibia o
         * digitado ao lado de um percentual medido contra outro.
         */
        var prescrito = AcompanhamentoCalculator.prescritoDeReferencia(
                volumeDaAvaliacao, r.getVolPrescrito24h());

        BigDecimal percentual = AcompanhamentoCalculator.percentualRecebido(
                r.getVolRecebido24h(), volumeDaAvaliacao, r.getVolPrescrito24h());

        BigDecimal kcal = AcompanhamentoCalculator.caloriasRecebidas(
                r.getVolRecebido24h(), densidade);
        BigDecimal ptn = AcompanhamentoCalculator.proteinaRecebida(
                r.getVolRecebido24h(), proteinaGL);

        return new RegistroDiarioUtiResponseDto(
                r.getId(),
                r.getPessoa().getId(),
                r.getPessoa().getNome(),
                a == null ? null : a.getId(),
                a == null ? null : a.getDataAvaliacao(),
                volumeDaAvaliacao,
                a == null ? null : a.getMetaEnergetica(),
                r.getData(),

                r.getDieta(), r.getVolPrescrito24h(), r.getVolRecebido24h(),

                r.getMg(), r.getK(), r.getNa(), r.getLactato(),
                r.getPcr(), r.getPh(), r.getPco2(), r.getHco3(), r.getHgt(),

                r.getSuporteVentilatorio(),
                r.getSuporteVentilatorio() == null
                        ? null : r.getSuporteVentilatorio().getDescricao(),
                r.getFio2Perc(), r.getPaSistolica(), r.getPaDiastolica(),
                r.getBalancoHidricoMl(), r.getDiureseMl(), r.getEvacuacao(),

                r.getCafeManha(), r.getLancheManha(), r.getAlmoco(),
                r.getLancheTarde(), r.getJantar(), r.getCeia(),

                arredondar(prescrito.valor()),
                arredondarPercentual(percentual),
                procedencia(prescrito),
                arredondar(kcal),
                arredondar(ptn),
                arredondar(porQuilo(kcal, peso)),
                arredondar(porQuilo(ptn, peso)),
                arredondar(AcompanhamentoCalculator.diuresePorQuiloHora(r.getDiureseMl(), peso)),
                arredondarPercentual(AcompanhamentoCalculator.mediaIngestaoOral(
                        r.getCafeManha(), r.getLancheManha(), r.getAlmoco(),
                        r.getLancheTarde(), r.getJantar(), r.getCeia())),
                a == null ? SEM_AVALIACAO : null,

                r.getObservacao(), r.getCreatedAt(), r.getUpdatedAt());
    }

    public static RegistroDiarioUtiListaDto toLista(RegistroDiarioUti r) {
        AvaliacaoUti a = r.getAvaliacao();
        BigDecimal peso = a == null ? null : a.getPesoTrabalhoKg();

        BigDecimal kcal = AcompanhamentoCalculator.caloriasRecebidas(
                r.getVolRecebido24h(), a == null ? null : a.getFormulaDensidadeKcalMl());

        var prescrito = AcompanhamentoCalculator.prescritoDeReferencia(
                a == null ? null : a.getVolumeTotalMl(), r.getVolPrescrito24h());

        return new RegistroDiarioUtiListaDto(
                r.getId(),
                r.getPessoa().getNome(),
                r.getData(),
                r.getVolPrescrito24h(),
                r.getVolRecebido24h(),
                arredondar(prescrito.valor()),
                procedencia(prescrito),
                arredondarPercentual(AcompanhamentoCalculator.percentualRecebido(
                        r.getVolRecebido24h(),
                        a == null ? null : a.getVolumeTotalMl(),
                        r.getVolPrescrito24h())),
                arredondar(porQuilo(kcal, peso)),
                r.getBalancoHidricoMl(),
                r.getDiureseMl(),
                a != null);
    }

    /**
     * A procedência acompanha o número, não o percentual.
     *
     * <p>Um dia com prescrito e ainda sem recebido — o dia que se abre de manhã —
     * passa a dizer "1.364 ml · prescrito na avaliação" em vez de mostrar o
     * volume sem nome. Onde havia percentual havia prescrito positivo, então
     * nenhuma resposta que já dizia algo passa a dizer outra coisa.
     */
    private static String procedencia(AcompanhamentoCalculator.PrescritoDeReferencia prescrito) {
        if (prescrito.valor() == null) return null;
        return prescrito.daAvaliacao() ? PRESCRITO_DA_AVALIACAO : PRESCRITO_DO_DIA;
    }

    private static BigDecimal porQuilo(BigDecimal total, BigDecimal pesoKg) {
        return positivo(pesoKg) ? dividir(total, pesoKg) : null;
    }
}
