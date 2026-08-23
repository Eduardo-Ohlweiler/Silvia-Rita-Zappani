package com.nutri.hospitalar.uti.calculo;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que as quatro ferramentas clínicas devolvem.
 *
 * <p>Como no cálculo, ausência vem <b>com o motivo</b>, e nunca um traço mudo.
 */
public record ResultadoFerramentas(
        Noradrenalina noradrenalina,
        BalancoNitrogenado balancoNitrogenado,
        Propofol propofol,
        Artesanal artesanal
) {

    /**
     * @param concentracaoMcgMl a concentração resultante da bolsa. Vai para a
     *                          tela porque é o número que se confere à beira do
     *                          leito — e é ele que mostra de onde saem os
     *                          "32" e "64" da planilha.
     * @param preparoDescricao  o preparo escolhido, por extenso
     */
    public record Noradrenalina(
            BigDecimal concentracaoMcgMl,
            BigDecimal doseMcgKgMin,
            String preparoDescricao,
            String motivo
    ) {}

    public record BalancoNitrogenado(
            BigDecimal nitrogenioIngerido,
            BigDecimal nitrogenioExcretado,
            BigDecimal balanco,
            Classificacao classificacao,
            String motivo
    ) {}

    /**
     * @param horasConsideradas as horas usadas na conta. Explícito porque na
     *                          planilha são a constante 24 escondida na fórmula.
     */
    public record Propofol(
            BigDecimal kcalDia,
            BigDecimal horasConsideradas,
            String motivo
    ) {}

    /**
     * @param kcalTotal     pela composição declarada dos produtos
     * @param kcalPorMacros pela soma dos macros (Atwater). Os dois têm de bater,
     *                      e vão os dois para a tela: é a conferência que a
     *                      própria planilha oferece.
     * @param percChoSobreVet os percentuais da planilha, cujo denominador é o
     *                        VET desejado — por isso somam 97,33 % e não 100
     * @param percChoSobreOfertado o denominador honesto, que soma 100
     */
    public record Artesanal(
            BigDecimal dosesBase,
            BigDecimal choTotal,
            BigDecimal ptnTotal,
            BigDecimal lipTotal,
            BigDecimal kcalBase,
            BigDecimal kcalTotal,
            BigDecimal kcalPorMacros,
            BigDecimal kcalPorQuilo,
            BigDecimal proteinaPorQuilo,
            BigDecimal percChoSobreVet,
            BigDecimal percPtnSobreVet,
            BigDecimal percLipSobreVet,
            BigDecimal percChoSobreOfertado,
            BigDecimal percPtnSobreOfertado,
            BigDecimal percLipSobreOfertado,
            BigDecimal aguaTotal,
            Integer administracoesPorDia,
            BigDecimal aguaPorAdministracao,
            List<Embalagem> embalagensPorMes,
            List<ItemReceita> receitaPorAdministracao,
            String motivo
    ) {}

    public record Embalagem(String produto, BigDecimal quantidade) {}

    public record ItemReceita(String produto, BigDecimal medidasPorDia,
                              BigDecimal medidasPorAdministracao) {}
}
