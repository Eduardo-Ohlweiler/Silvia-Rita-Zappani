package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CEM;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.dividir;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.kcalDeMacros;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.percentual;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * Dieta artesanal — sistema aberto. Especificação: {@code docs/10} §7.
 *
 * <p>Quatro papéis fixos na receita: <b>base</b> (o pó que carrega o volume),
 * <b>carboidrato</b>, <b>proteína</b> e <b>lipídio</b>. Quem ocupa cada papel
 * vem do catálogo ({@code produto_nutricional.papel_artesanal}), não de
 * constante no código — no eroERP os quatro produtos estão cravados em
 * {@code calculoSistemaAberto.ts:6-11} e trocar de marca exige recompilar.
 *
 * <p>Os papéis é que ficam fixos, porque a lógica de cada um é específica: a
 * base se auto-sugere pelo VET, e o carboidrato entra <b>fora</b> do fechamento
 * de kcal da base. Generalizar para "quatro produtos quaisquer" seria invenção
 * nossa sobre comportamento clínico.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco.
 */
public final class DietaArtesanalCalculator {

    private DietaArtesanalCalculator() {}

    /**
     * Fator de ajuste da sugestão de doses da base ({@code TNE SA!B9}).
     *
     * <p>A planilha sugere 85 % do que fecharia o VET exato. Não há nota
     * explicando; a leitura provável é margem para o que os outros insumos
     * somam. Reproduzimos o número da fonte.
     */
    private static final BigDecimal FATOR_SUGESTAO = new BigDecimal("0.85");

    /** ml de água por medida da base ({@code TNE SA!B15}). */
    private static final BigDecimal AGUA_POR_MEDIDA_BASE = new BigDecimal("28.5");

    /** ml de água fixos, e por medida de carboidrato. */
    private static final BigDecimal AGUA_FIXA = new BigDecimal("100");
    private static final BigDecimal AGUA_POR_MEDIDA_CARBOIDRATO = new BigDecimal("100");

    /** Dias considerados no cálculo de embalagens ({@code TNE SA!B17:B20}). */
    private static final BigDecimal DIAS_DO_MES = new BigDecimal("31");

    // ─── Sugestão de doses da base ──────────────────────────────────────

    /**
     * Quantas medidas da base cobrem o VET, descontando o que o óleo já entrega.
     *
     * <pre>
     * doses = ((VET − LIP_do_óleo × 9) / kcal_por_medida_da_base) × 0,85
     * </pre>
     *
     * <p>Confere, VET 2000 com 0,5 colher de óleo (6 g de LIP) e base de
     * 33,93 kcal/medida: {@code (2000 − 54) / 33,93 × 0,85 = 48,7504}.
     */
    public static BigDecimal dosesSugeridas(BigDecimal vetDesejado, BigDecimal lipidioDoOleoG,
                                            BigDecimal kcalPorMedidaDaBase) {
        if (!positivo(vetDesejado) || !positivo(kcalPorMedidaDaBase)) return null;

        BigDecimal kcalDoOleo = kcalDeMacros(null, null, lipidioDoOleoG);

        return vetDesejado.subtract(kcalDoOleo, CONTA)
                .divide(kcalPorMedidaDaBase, CONTA)
                .multiply(FATOR_SUGESTAO, CONTA);
    }

    // ─── A receita completa ─────────────────────────────────────────────

    /**
     * Monta a receita e devolve tudo o que a tela precisa.
     *
     * <p><b>A validação aritmética mais forte do sistema mora aqui.</b> A
     * planilha calcula a kcal total por dois caminhos independentes — soma dos
     * macros por Atwater, e kcal da base mais a do carboidrato — e os dois dão
     * 1946,6498. Isso confirma {@link UtiMatematica#kcalDeMacros} contra a
     * fonte, e por isso o resultado carrega os dois: {@code kcalTotal} e
     * {@code kcalPorMacros}, que o teste exige idênticos.
     *
     * @param vetDesejado          kcal/dia que se quer entregar
     * @param dosesBaseInformadas  medidas da base; {@code null} pede a sugestão
     * @param administracoesPorDia em quantas vezes a receita é dividida
     */
    public static ResultadoArtesanal calcular(BigDecimal vetDesejado, PesoDeTrabalho peso,
                                              InsumoArtesanal base, BigDecimal dosesBaseInformadas,
                                              InsumoArtesanal carboidrato, BigDecimal medidasCarboidrato,
                                              InsumoArtesanal proteina, BigDecimal medidasProteina,
                                              InsumoArtesanal lipidio, BigDecimal medidasLipidio,
                                              Integer administracoesPorDia) {
        if (base == null || !positivo(vetDesejado)) return null;

        BigDecimal lipidioDoOleo = contribuicao(lipidio, medidasLipidio, InsumoArtesanal::lipG);

        BigDecimal dosesBase = dosesBaseInformadas != null
                ? dosesBaseInformadas
                : dosesSugeridas(vetDesejado, lipidioDoOleo, base.kcal());
        if (dosesBase == null) return null;

        // ─── Macros de cada insumo ───────────────────────────────────────
        BigDecimal choBase = contribuicao(base, dosesBase, InsumoArtesanal::choG);
        BigDecimal ptnBase = contribuicao(base, dosesBase, InsumoArtesanal::ptnG);
        BigDecimal lipBase = contribuicao(base, dosesBase, InsumoArtesanal::lipG);

        BigDecimal choCarbo = contribuicao(carboidrato, medidasCarboidrato, InsumoArtesanal::choG);
        BigDecimal choPtn   = contribuicao(proteina, medidasProteina, InsumoArtesanal::choG);
        BigDecimal ptnPtn   = contribuicao(proteina, medidasProteina, InsumoArtesanal::ptnG);

        BigDecimal choTotal = choBase.add(choCarbo, CONTA).add(choPtn, CONTA);
        BigDecimal ptnTotal = ptnBase.add(ptnPtn, CONTA);
        BigDecimal lipTotal = lipBase.add(lipidioDoOleo, CONTA);

        // ─── Energia, pelos dois caminhos ────────────────────────────────
        // O carboidrato puro fica FORA do fechamento da base e entra depois pela
        // sua própria kcal — é assim que a planilha faz (TNE SA!B12 e B14).
        BigDecimal kcalBase = kcalDeMacros(choBase.add(choPtn, CONTA), ptnTotal, lipTotal);
        BigDecimal kcalDoCarboidrato = contribuicao(carboidrato, medidasCarboidrato,
                InsumoArtesanal::kcal);
        BigDecimal kcalTotal = kcalBase.add(kcalDoCarboidrato, CONTA);

        BigDecimal kcalPorMacros = kcalDeMacros(choTotal, ptnTotal, lipTotal);

        // ─── Água ────────────────────────────────────────────────────────
        BigDecimal agua = AGUA_POR_MEDIDA_BASE.multiply(dosesBase, CONTA)
                .add(AGUA_FIXA, CONTA)
                .add(AGUA_POR_MEDIDA_CARBOIDRATO.multiply(
                        zeroSeNulo(medidasCarboidrato), CONTA), CONTA);

        int administracoes = administracoesPorDia == null || administracoesPorDia <= 0
                ? 4 : administracoesPorDia;
        BigDecimal aguaPorAdministracao = dividir(agua, new BigDecimal(administracoes));

        // ─── Embalagens por mês ──────────────────────────────────────────
        List<EmbalagensPorMes> embalagens = new ArrayList<>();
        adicionarEmbalagem(embalagens, base, dosesBase);
        adicionarEmbalagem(embalagens, carboidrato, medidasCarboidrato);
        adicionarEmbalagem(embalagens, proteina, medidasProteina);
        adicionarEmbalagem(embalagens, lipidio, medidasLipidio);

        // ─── Receita por administração ───────────────────────────────────
        BigDecimal porAdministracao = new BigDecimal(administracoes);
        List<ItemDaReceita> receita = new ArrayList<>();
        adicionarItem(receita, base, dosesBase, porAdministracao);
        adicionarItem(receita, carboidrato, medidasCarboidrato, porAdministracao);
        adicionarItem(receita, proteina, medidasProteina, porAdministracao);
        adicionarItem(receita, lipidio, medidasLipidio, porAdministracao);

        return new ResultadoArtesanal(
                dosesBase,
                choBase, ptnBase, lipBase,
                choTotal, ptnTotal, lipTotal,
                kcalBase, kcalTotal, kcalPorMacros,
                peso == null ? null : dividir(kcalTotal, peso.valorKg()),
                peso == null ? null : dividir(ptnTotal, peso.valorKg()),
                percentual(kcalDeMacros(choTotal, null, null), vetDesejado),
                percentual(kcalDeMacros(null, ptnTotal, null), vetDesejado),
                percentual(kcalDeMacros(null, null, lipTotal), vetDesejado),
                percentual(kcalDeMacros(choTotal, null, null), kcalTotal),
                percentual(kcalDeMacros(null, ptnTotal, null), kcalTotal),
                percentual(kcalDeMacros(null, null, lipTotal), kcalTotal),
                agua, administracoes, aguaPorAdministracao,
                List.copyOf(embalagens), List.copyOf(receita));
    }

    // ─────────────────────────────────────────────────────────────────────

    private static BigDecimal contribuicao(InsumoArtesanal insumo, BigDecimal medidas,
                                           java.util.function.Function<InsumoArtesanal, BigDecimal> campo) {
        if (insumo == null || medidas == null) return BigDecimal.ZERO;
        BigDecimal valor = campo.apply(insumo);
        return valor == null ? BigDecimal.ZERO : valor.multiply(medidas, CONTA);
    }

    /**
     * {@code (medidas/dia × g_por_medida / g_por_embalagem) × 31}.
     *
     * <p>Usa a medida <b>declarada no catálogo</b>. Na planilha o óleo é contado
     * com 10 ml onde a própria tabela declara 13 ml ({@code TNE SA!B20},
     * defeito 11) — aqui não há como o número da conta divergir do número do
     * cadastro.
     */
    private static void adicionarEmbalagem(List<EmbalagensPorMes> destino,
                                           InsumoArtesanal insumo, BigDecimal medidas) {
        if (insumo == null || !positivo(medidas) || !positivo(insumo.embalagemQtd())) return;

        BigDecimal quantidade = medidas.multiply(insumo.medidaQtd(), CONTA)
                .divide(insumo.embalagemQtd(), CONTA)
                .multiply(DIAS_DO_MES, CONTA);

        destino.add(new EmbalagensPorMes(insumo.nome(), quantidade));
    }

    /**
     * Quanto de cada insumo entra em <b>uma</b> administração.
     *
     * <p>Sempre {@code medidas / nº de administrações} — inclusive para o óleo.
     * Na planilha, {@code TNE SA!B27} é a constante {@code 13/4}: a receita de
     * óleo por dose <b>não muda</b> quando se muda a quantidade de óleo, porque
     * ela ignora a entrada que todas as outras fórmulas usam (defeito 12).
     */
    private static void adicionarItem(List<ItemDaReceita> destino, InsumoArtesanal insumo,
                                      BigDecimal medidas, BigDecimal administracoes) {
        if (insumo == null || !positivo(medidas)) return;
        destino.add(new ItemDaReceita(insumo.nome(),
                medidas, dividir(medidas, administracoes)));
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    // ─── Tipos ──────────────────────────────────────────────────────────

    /**
     * Um insumo do catálogo, já resolvido pelo service.
     *
     * <p>Composição <b>por medida</b>, como o rótulo declara — a mesma unidade
     * de {@code produto_nutricional}.
     */
    public record InsumoArtesanal(String nome, BigDecimal medidaQtd, BigDecimal embalagemQtd,
                                  BigDecimal kcal, BigDecimal choG, BigDecimal ptnG,
                                  BigDecimal lipG) {}

    public record EmbalagensPorMes(String produto, BigDecimal quantidade) {}

    /**
     * @param medidasPorDia          total do dia
     * @param medidasPorAdministracao o que vai em cada uma
     */
    public record ItemDaReceita(String produto, BigDecimal medidasPorDia,
                                BigDecimal medidasPorAdministracao) {}

    /**
     * @param kcalTotal      pela composição declarada dos produtos
     * @param kcalPorMacros  pela soma dos macros (Atwater) — <b>tem de ser
     *                       idêntico a {@code kcalTotal}</b>, e há teste que
     *                       exige isso. É a validação cruzada que a própria
     *                       planilha oferece.
     * @param percChoSobreVet  o número da planilha: denominador é o VET desejado,
     *                         e por isso os três somam 97,33 % e não 100
     * @param percChoSobreOfertado o denominador honesto: soma 100 por construção.
     *                             Expomos os dois, nomeados — quem vê 52,2 + 18,5
     *                             + 26,6 e não sabe do denominador acha que 2,7 %
     *                             sumiram
     */
    public record ResultadoArtesanal(
            BigDecimal dosesBase,
            BigDecimal choDaBase, BigDecimal ptnDaBase, BigDecimal lipDaBase,
            BigDecimal choTotal, BigDecimal ptnTotal, BigDecimal lipTotal,
            BigDecimal kcalBase, BigDecimal kcalTotal, BigDecimal kcalPorMacros,
            BigDecimal kcalPorQuilo, BigDecimal proteinaPorQuilo,
            BigDecimal percChoSobreVet, BigDecimal percPtnSobreVet, BigDecimal percLipSobreVet,
            BigDecimal percChoSobreOfertado, BigDecimal percPtnSobreOfertado,
            BigDecimal percLipSobreOfertado,
            BigDecimal aguaTotal, int administracoesPorDia, BigDecimal aguaPorAdministracao,
            List<EmbalagensPorMes> embalagensPorMes,
            List<ItemDaReceita> receitaPorAdministracao) {}
}
