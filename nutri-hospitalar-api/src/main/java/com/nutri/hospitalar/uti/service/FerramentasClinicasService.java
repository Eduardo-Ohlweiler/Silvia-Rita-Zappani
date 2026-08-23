package com.nutri.hospitalar.uti.service;

import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.uti.calculo.DietaArtesanalCalculator;
import com.nutri.hospitalar.uti.calculo.FerramentaClinicaCalculator;
import com.nutri.hospitalar.uti.calculo.ResultadoFerramentas;
import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;
import com.nutri.hospitalar.uti.dtos.FerramentasClinicasRequestDto;
import com.nutri.hospitalar.uti.entity.ProdutoNutricional;
import com.nutri.hospitalar.uti.enums.PreparoNoradrenalina;
import com.nutri.hospitalar.uti.repository.ProdutoNutricionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.arredondar;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.arredondarPercentual;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * As quatro ferramentas clínicas: dose de noradrenalina, balanço nitrogenado,
 * calorias do propofol e receita da dieta artesanal.
 *
 * <p><b>Nada é gravado, e nada é logado.</b> É tela de conferência avulsa, e o
 * que passa por aqui é dado de saúde (regra 5 do {@code CLAUDE.md}).
 *
 * <p>O que este service faz além de chamar os calculadores é resolver os
 * <b>insumos da dieta artesanal no catálogo</b> — e é isso que mata o hardcode:
 * no eroERP os quatro produtos estão cravados em
 * {@code calculoSistemaAberto.ts:6-11}, e trocar de marca exige recompilar.
 */
@Service
@RequiredArgsConstructor
public class FerramentasClinicasService {

    private static final String SEM_PESO_E_VAZAO =
            "Informe o peso e a vazão da bomba";
    private static final String SEM_PREPARO =
            "Informe as ampolas e o volume do soro do preparo";
    private static final String SEM_BALANCO =
            "Informe a proteína ingerida e a ureia urinária de 24 horas";
    private static final String SEM_PROPOFOL =
            "Informe a vazão do propofol";
    private static final String SEM_BASE =
            "Escolha o insumo base e informe o VET desejado";

    private final ProdutoNutricionalRepository produtoNutricionalRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public ResultadoFerramentas calcular(FerramentasClinicasRequestDto dto) {
        return new ResultadoFerramentas(
                noradrenalina(dto),
                balancoNitrogenado(dto),
                propofol(dto),
                artesanal(dto));
    }

    // ─── Noradrenalina ──────────────────────────────────────────────────

    /**
     * Uma fórmula, três caminhos até ela.
     *
     * <p>Os presets fornecem ampolas e soro; o preparo do serviço vem do
     * formulário. A concentração resultante sobe para a tela nos três casos —
     * é o que torna os "32" e "64" conferíveis em vez de mágicos.
     */
    private ResultadoFerramentas.Noradrenalina noradrenalina(FerramentasClinicasRequestDto d) {
        PreparoNoradrenalina preparo = d.noraPreparo() == null
                ? PreparoNoradrenalina.SIMPLES_32 : d.noraPreparo();

        BigDecimal ampolas = preparo.exigeAmpolasESoro() ? d.noraAmpolas() : preparo.getAmpolas();
        BigDecimal soro = preparo.exigeAmpolasESoro() ? d.noraVolumeSoroMl() : preparo.getVolumeSoroMl();

        BigDecimal concentracao = FerramentaClinicaCalculator
                .concentracaoNoradrenalina(ampolas, soro);

        if (concentracao == null)
            return new ResultadoFerramentas.Noradrenalina(
                    null, null, preparo.getDescricao(), SEM_PREPARO);

        BigDecimal dose = positivo(d.noraPesoKg())
                ? FerramentaClinicaCalculator.doseNoradrenalina(
                        d.noraVazaoMlH(), concentracao, PesoDeTrabalho.informado(d.noraPesoKg()))
                : null;

        return new ResultadoFerramentas.Noradrenalina(
                arredondar(concentracao), arredondar(dose),
                preparo.getDescricao(),
                dose == null ? SEM_PESO_E_VAZAO : null);
    }

    // ─── Balanço nitrogenado ────────────────────────────────────────────

    private ResultadoFerramentas.BalancoNitrogenado balancoNitrogenado(
            FerramentasClinicasRequestDto d) {

        var balanco = FerramentaClinicaCalculator.balancoNitrogenado(
                d.balancoProteinaG(), d.balancoUreiaG());

        if (balanco == null)
            return new ResultadoFerramentas.BalancoNitrogenado(
                    null, null, null, null, SEM_BALANCO);

        return new ResultadoFerramentas.BalancoNitrogenado(
                arredondar(balanco.nitrogenioIngerido()),
                arredondar(balanco.nitrogenioExcretado()),
                arredondar(balanco.balanco()),
                balanco.classificacao(),
                null);
    }

    // ─── Propofol ───────────────────────────────────────────────────────

    private ResultadoFerramentas.Propofol propofol(FerramentasClinicasRequestDto d) {
        BigDecimal horas = d.propofolHoras() == null
                ? FerramentaClinicaCalculator.HORAS_PROPOFOL_PADRAO : d.propofolHoras();

        BigDecimal kcal = FerramentaClinicaCalculator.caloriasPropofol(d.propofolVazaoMlH(), horas);

        return new ResultadoFerramentas.Propofol(
                arredondar(kcal), horas, kcal == null ? SEM_PROPOFOL : null);
    }

    // ─── Dieta artesanal ────────────────────────────────────────────────

    private ResultadoFerramentas.Artesanal artesanal(FerramentasClinicasRequestDto d) {
        var base = buscarInsumo(d.insumoBaseId());

        if (base == null || !positivo(d.artesanalVetKcal()))
            return vaziaComMotivo();

        var resultado = DietaArtesanalCalculator.calcular(
                d.artesanalVetKcal(),
                positivo(d.artesanalPesoKg()) ? PesoDeTrabalho.informado(d.artesanalPesoKg()) : null,
                base, d.dosesBase(),
                buscarInsumo(d.insumoCarboidratoId()), d.medidasCarboidrato(),
                buscarInsumo(d.insumoProteinaId()), d.medidasProteina(),
                buscarInsumo(d.insumoLipidioId()), d.medidasLipidio(),
                d.administracoesPorDia());

        if (resultado == null) return vaziaComMotivo();

        return new ResultadoFerramentas.Artesanal(
                arredondar(resultado.dosesBase()),
                arredondar(resultado.choTotal()),
                arredondar(resultado.ptnTotal()),
                arredondar(resultado.lipTotal()),
                arredondar(resultado.kcalBase()),
                arredondar(resultado.kcalTotal()),
                arredondar(resultado.kcalPorMacros()),
                arredondar(resultado.kcalPorQuilo()),
                arredondar(resultado.proteinaPorQuilo()),
                arredondarPercentual(resultado.percChoSobreVet()),
                arredondarPercentual(resultado.percPtnSobreVet()),
                arredondarPercentual(resultado.percLipSobreVet()),
                arredondarPercentual(resultado.percChoSobreOfertado()),
                arredondarPercentual(resultado.percPtnSobreOfertado()),
                arredondarPercentual(resultado.percLipSobreOfertado()),
                arredondar(resultado.aguaTotal()),
                resultado.administracoesPorDia(),
                arredondar(resultado.aguaPorAdministracao()),
                resultado.embalagensPorMes().stream()
                        .map(e -> new ResultadoFerramentas.Embalagem(
                                e.produto(), arredondar(e.quantidade())))
                        .toList(),
                resultado.receitaPorAdministracao().stream()
                        .map(i -> new ResultadoFerramentas.ItemReceita(
                                i.produto(), arredondar(i.medidasPorDia()),
                                arredondar(i.medidasPorAdministracao())))
                        .toList(),
                null);
    }

    private ResultadoFerramentas.Artesanal vaziaComMotivo() {
        return new ResultadoFerramentas.Artesanal(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, List.of(), List.of(), SEM_BASE);
    }

    /**
     * O insumo, buscado no catálogo dentro do tenant.
     *
     * <p>Enxerga o do cliente e o global. Produto de outro cliente simplesmente
     * não é encontrado, e o cálculo trata a ausência — aqui não vale devolver
     * 404, porque a tela recalcula a cada tecla e um id inválido não deve
     * derrubar as outras três ferramentas.
     */
    private DietaArtesanalCalculator.InsumoArtesanal buscarInsumo(UUID id) {
        if (id == null) return null;

        UUID tenantId = securityUtils.getTenantIdLogado();

        return produtoNutricionalRepository.findByIdVisivelPara(id, tenantId)
                .map(FerramentasClinicasService::paraInsumo)
                .orElse(null);
    }

    private static DietaArtesanalCalculator.InsumoArtesanal paraInsumo(ProdutoNutricional p) {
        return new DietaArtesanalCalculator.InsumoArtesanal(
                p.getNome(), p.getMedidaQtd(), p.getEmbalagemQtd(),
                p.getKcal(), p.getChoG(), p.getProteinaG(), p.getLipG());
    }
}
