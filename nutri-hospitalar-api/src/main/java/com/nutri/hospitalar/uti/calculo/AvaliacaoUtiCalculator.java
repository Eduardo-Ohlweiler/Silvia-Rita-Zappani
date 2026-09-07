package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.calculo.cascata.Altura;
import com.nutri.hospitalar.uti.calculo.cascata.MetaEnergetica;
import com.nutri.hospitalar.uti.calculo.cascata.MetaProteica;
import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;
import com.nutri.hospitalar.uti.calculo.cascata.VolumeDieta;
import com.nutri.hospitalar.uti.enums.ModoInfusao;
import com.nutri.hospitalar.uti.enums.OrigemValor;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.enums.PosicaoNaFaixa;
import com.nutri.hospitalar.uti.enums.TerapiaRenal;
import com.nutri.hospitalar.uti.enums.TomResultado;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.arredondar;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.arredondarPercentual;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * A cascata, pura: das medidas do paciente até o volume de dieta e a água extra.
 *
 * <p><b>Esta classe existe porque a planilha não tem cascata.</b> Lá o peso está
 * digitado em oito células independentes, a altura em quatro, e a meta calórica
 * é redigitada à mão em cada aba — defeitos 14 e 15 de {@code docs/10} §11.
 * Aqui há <b>um</b> peso de trabalho, <b>uma</b> altura e <b>uma</b> meta, cada
 * um construído uma vez e passado adiante com a sua origem colada.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco. O que depende do banco — a
 * fórmula enteral e o P50 da circunferência do braço — chega resolvido, como
 * {@code CalculoPediatricoCalculator} recebe a linha de percentil.
 */
public final class AvaliacaoUtiCalculator {

    private AvaliacaoUtiCalculator() {}

    private static final BigDecimal IMC_ALVO_HOMEM  = new BigDecimal("22");
    private static final BigDecimal IMC_ALVO_MULHER = new BigDecimal("20.8");
    private static final BigDecimal IMC_ALVO_SUPERIOR = new BigDecimal("25");

    /** Abaixo daqui as duas colunas de ajuste de CB e CP divergem. */
    private static final BigDecimal IMC_LIMITE_MAGREZA = new BigDecimal("18.5");

    private static final String ESCOLHA_MANUAL =
            "Coluna escolhida no formulário";

    // Motivos de ausência — o texto que a tela mostra no lugar do traço.
    private static final String SEM_PESO =
            "Informe o peso, ou as medidas para estimá-lo (braço e panturrilha, ou altura do joelho)";
    private static final String SEM_ALTURA =
            "Informe a altura, ou a altura do joelho para estimá-la";
    private static final String SEM_SEXO =
            "Informe o sexo: as equações de estimativa são separadas por sexo";
    private static final String SEM_MEDIDAS_PARA_ESTIMAR =
            "Informe altura do joelho e circunferência do braço para as estimativas de peso";
    /**
     * A origem foi escolhida à mão, a equação rodou, e o que ela devolveu não é
     * peso.
     *
     * <p>Não pode ser {@link #SEM_PESO}: aquele texto manda informar medidas que
     * o profissional <b>já informou</b>, e culpar o dado que está lá é pior que
     * não dizer nada. Aqui as medidas existem — é o resultado delas que não
     * fecha, e o que resolve é conferir a vírgula.
     */
    private static final String ESTIMATIVA_IMPLAUSIVEL =
            "A estimativa escolhida não produziu um peso plausível com estas medidas — "
                    + "confira a circunferência do braço e a altura do joelho, ou informe o peso";
    private static final String SEM_PESO_USUAL =
            "Informe o peso habitual e a janela de tempo para avaliar a perda";
    private static final String SEM_P50 =
            "A tabela de percentil da circunferência do braço vai de 18 a 90,9 anos";
    private static final String SEM_FORMULA =
            "Escolha a fórmula enteral para calcular o que a dieta entrega";
    private static final String SEM_VOLUME_OU_TEMPO =
            "Informe o volume e o tempo de infusão";
    private static final String SEM_FASE =
            "Escolha a fase da terapia, ou informe um alvo em kcal/kg";
    private static final String SEM_DENSIDADE_NA_TABELA =
            "Esta densidade não tem linha na tabela de água. Cadastre a água livre no rótulo da fórmula.";
    private static final String META_PROTEICA_ATINGIDA =
            "A dieta já cobre a meta proteica — não há lacuna a suplementar";
    private static final String SEM_MODULO =
            "Escolha um módulo proteico para ver quanto dele cobre a lacuna";
    private static final String MODULO_SEM_COMPOSICAO =
            "%s não tem medida, proteína ou calorias no cadastro — sem elas não há o que sugerir";
    private static final String SEM_META_PROTEICA =
            "Sem meta proteica não há lacuna a medir — escolha a fase da terapia, "
                    + "ou informe um alvo em g/kg";

    /**
     * @param formula fórmula enteral escolhida, ou {@code null}
     * @param modulo  módulo proteico escolhido para cobrir a lacuna, ou
     *                {@code null}. Não influencia nenhum outro número: a
     *                sugestão é leitura da lacuna que a dieta já produziu
     * @param p50CircBracoCm P50 da tabela para o sexo e a idade, ou {@code null}
     *                       quando a idade está fora de 18 a 90,9 anos
     */
    public static ResultadoUti calcular(EntradaUti entrada, FormulaEnteralResolvida formula,
                                        ModuloProteicoResolvido modulo,
                                        BigDecimal p50CircBracoCm) {

        Altura altura = resolverAltura(entrada);
        PesoDeTrabalho peso = resolverPeso(entrada);

        ResultadoUti.Antropometria antropometria = antropometria(entrada, altura, peso, p50CircBracoCm);
        BigDecimal imc = antropometria.imc();

        Metas metas = necessidades(entrada, peso, imc, altura);

        ResultadoUti.Dieta dieta = dieta(entrada, formula, modulo, peso,
                metas.energetica(), metas.proteica());
        ResultadoUti.Hidratacao hidratacao = hidratacao(entrada, formula, peso, dieta);

        return new ResultadoUti(antropometria, metas.dto(), dieta, hidratacao);
    }

    // ─── A cascata: altura e peso, uma vez cada ─────────────────────────

    /** Informada vence a estimada — e a estimada declara que é estimada. */
    private static Altura resolverAltura(EntradaUti e) {
        if (positivo(e.alturaCm()))
            return Altura.informadaEmCentimetros(e.alturaCm());

        BigDecimal estimada = AntropometriaCalculator.alturaChumlea1985(
                e.alturaJoelhoCm(), e.idadeAnos(), e.sexo());

        return estimada == null ? null : Altura.estimadaEmCentimetros(estimada);
    }

    /**
     * O peso que <b>todo</b> o resto vai usar.
     *
     * <p>Ordem: a fonte que o profissional escolheu, se escolheu; senão o peso
     * informado; senão a primeira estimativa possível, na ordem <b>Rabito →
     * Chumlea → Jung</b>.
     *
     * <p><b>A ordem é convenção nossa, declarada.</b> Rabito vem primeiro porque
     * usa três circunferências e nenhuma altura de joelho — é a que mais
     * frequentemente tem todas as medidas à beira do leito. A planilha não
     * escolhe: o peso estimado dela não alimenta aba nenhuma (defeito 20).
     */
    private static PesoDeTrabalho resolverPeso(EntradaUti e) {
        if (e.origemPesoPreferida() != null) {
            BigDecimal escolhido = pesoDaOrigem(e, e.origemPesoPreferida());
            /*
             * `positivo`, e não `!= null`. O ramo automático abaixo sempre
             * testou o sinal; este, o da origem ESCOLHIDA no seletor, testava
             * só a nulidade — e as equações de estimativa são lineares com uma
             * constante grande subtraída, então medida deslocada pela máscara
             * devolve peso NEGATIVO, não nulo. Com circunferência de braço de
             * 0,32 cm (digitou "32"), Chumlea 1988 dá peso negativo para toda
             * combinação de sexo e etnia e qualquer altura de joelho, inclusive
             * a correta. O record `PesoDeTrabalho` recusa não-positivo com
             * IllegalArgumentException, que não tem handler: virava HTTP 500,
             * "Erro interno", em produção, na tela de quem prescreve.
             */
            return positivo(escolhido)
                    ? new PesoDeTrabalho(escolhido, e.origemPesoPreferida())
                    : null;
        }

        if (positivo(e.pesoAtualKg()))
            return PesoDeTrabalho.informado(e.pesoAtualKg());

        for (OrigemValor origem : List.of(OrigemValor.ESTIMADO_RABITO,
                                          OrigemValor.ESTIMADO_CHUMLEA,
                                          OrigemValor.ESTIMADO_JUNG)) {
            BigDecimal estimado = pesoDaOrigem(e, origem);
            if (positivo(estimado)) return new PesoDeTrabalho(estimado, origem);
        }

        return null;
    }

    /**
     * Por que não há peso de trabalho — e são dois motivos diferentes.
     *
     * <p>Se uma origem foi escolhida e ela <b>produziu</b> um número (não nulo)
     * e ainda assim não virou peso, o número era não-positivo: as medidas estão
     * lá e o resultado é que é implausível. Só quando não há sequer o que
     * calcular é que cabe pedir as medidas.
     */
    private static String motivoDoPesoAusente(EntradaUti e) {
        return e.origemPesoPreferida() != null
                && pesoDaOrigem(e, e.origemPesoPreferida()) != null
                ? ESTIMATIVA_IMPLAUSIVEL
                : SEM_PESO;
    }

    private static BigDecimal pesoDaOrigem(EntradaUti e, OrigemValor origem) {
        return switch (origem) {
            case INFORMADO -> e.pesoAtualKg();
            case ESTIMADO_RABITO -> EstimativaPesoCalculator.rabito2008(
                    e.circBracoCm(), e.circAbdominalCm(), e.circPanturrilhaCm(), e.sexo());
            case ESTIMADO_CHUMLEA -> EstimativaPesoCalculator.chumlea1988(
                    e.alturaJoelhoCm(), e.circBracoCm(), e.idadeAnos(), e.sexo(), e.etnia());
            case ESTIMADO_JUNG -> EstimativaPesoCalculator.jung2004(
                    e.alturaJoelhoCm(), e.circBracoCm(), e.idadeAnos(), e.sexo());
            default -> null;
        };
    }

    // ─── Aba 1 — Antropometria ──────────────────────────────────────────

    private static ResultadoUti.Antropometria antropometria(EntradaUti e, Altura altura,
                                                            PesoDeTrabalho peso,
                                                            BigDecimal p50) {
        // A população de referência é decidida depois da perda de peso: é ela
        // que a justifica (docs/10 §2.9).

        // Estimativas
        BigDecimal alturaEstimada = AntropometriaCalculator.alturaChumlea1985(
                e.alturaJoelhoCm(), e.idadeAnos(), e.sexo());
        BigDecimal chumlea = EstimativaPesoCalculator.chumlea1988(
                e.alturaJoelhoCm(), e.circBracoCm(), e.idadeAnos(), e.sexo(), e.etnia());
        BigDecimal jung = EstimativaPesoCalculator.jung2004(
                e.alturaJoelhoCm(), e.circBracoCm(), e.idadeAnos(), e.sexo());
        BigDecimal rabito = EstimativaPesoCalculator.rabito2008(
                e.circBracoCm(), e.circAbdominalCm(), e.circPanturrilhaCm(), e.sexo());

        String motivoEstimativas = null;
        if (e.sexo() == null) motivoEstimativas = SEM_SEXO;
        else if (chumlea == null && jung == null && rabito == null)
            motivoEstimativas = SEM_MEDIDAS_PARA_ESTIMAR;

        // IMC
        BigDecimal imc = peso == null || altura == null
                ? null : AntropometriaCalculator.imc(peso.valorKg(), altura);
        String motivoImc = imc != null ? null
                : (peso == null ? motivoDoPesoAusente(e) : SEM_ALTURA);

        // Metas de peso
        BigDecimal imcAlvo = e.sexo() == Sexo.FEMININO ? IMC_ALVO_MULHER : IMC_ALVO_HOMEM;
        BigDecimal pesoIdeal = altura == null || e.sexo() == null
                ? null : AntropometriaCalculator.pesoIdealPorImc(imcAlvo, altura);
        BigDecimal pesoIdeal25 = altura == null
                ? null : AntropometriaCalculator.pesoIdealPorImc(IMC_ALVO_SUPERIOR, altura);
        BigDecimal pesoAjustado = peso == null || pesoIdeal == null
                ? null : AntropometriaCalculator.pesoAjustado(peso.valorKg(), pesoIdeal);
        BigDecimal pesoAmputacao = peso == null || e.segmentosOuVazio().isEmpty()
                ? null : AntropometriaCalculator.pesoCorrigidoPorAmputacao(
                        peso.valorKg(), e.segmentosOuVazio());

        // Perda de peso
        BigDecimal perda = AntropometriaCalculator.percentualPerdaPeso(
                e.pesoUsualKg(), peso == null ? null : peso.valorKg());
        Classificacao classifPerda = AntropometriaCalculator.classificarPerdaPeso(
                perda, e.janelaPerda());
        String motivoPerda = classifPerda != null ? null : SEM_PESO_USUAL;

        // Qual coluna de ajuste usar, e por quê (docs/10 §2.9).
        //
        // A população clínica já é o padrão do módulo, então o automatismo não
        // muda número nenhum — ele existe para a tela DIZER que a perda de peso
        // registrada é o que sustenta a escolha. Sem isso o profissional vê o
        // seletor sem saber o que o justifica. A troca manual continua valendo
        // e também fica escrita.
        boolean perdaSignificativa = classifPerda != null
                && (classifPerda.tom() == TomResultado.ATENCAO
                    || classifPerda.tom() == TomResultado.CRITICO);

        PopulacaoReferencia populacao;
        String motivoPopulacao;
        if (e.populacaoReferencia() != null) {
            populacao = e.populacaoReferencia();
            motivoPopulacao = ESCOLHA_MANUAL;
        } else if (perdaSignificativa) {
            populacao = PopulacaoReferencia.POPULACAO_CLINICA;
            motivoPopulacao = "%s: somar centímetro no magro mascararia a depleção"
                    .formatted(classifPerda.rotulo());
        } else {
            populacao = e.populacaoOuPadrao();
            motivoPopulacao = null;
        }

        // Adequação de CB
        BigDecimal adequacao = AntropometriaCalculator.adequacaoCircBraco(e.circBracoCm(), p50);
        String motivoAdequacao = adequacao != null ? null
                : (p50 == null ? SEM_P50 : "Informe a circunferência do braço");

        // Depleção, com o ajuste pelo IMC
        BigDecimal cbAjustada = AntropometriaCalculator.ajustarCircBraco(
                e.circBracoCm(), imc, e.sexo(), populacao);
        BigDecimal cpAjustada = AntropometriaCalculator.ajustarCircPanturrilha(
                e.circPanturrilhaCm(), imc, populacao);
        // Um motivo POR MEDIDA. Antes havia um só, que existia apenas quando as
        // DUAS faltavam — então a panturrilha sozinha ficava muda — e que
        // culpava o IMC mesmo quando o que faltava era a circunferência.
        String motivoMassaBraco = motivoDoAjuste(
                cbAjustada, e.circBracoCm(), imc, e.sexo(), "do braço");
        String motivoDeplecaoPanturrilha = motivoDoAjuste(
                cpAjustada, e.circPanturrilhaCm(), imc, e.sexo(), "da panturrilha");

        return new ResultadoUti.Antropometria(
                arredondar(alturaEstimada), arredondar(chumlea), arredondar(jung),
                arredondar(rabito), motivoEstimativas,

                peso == null ? null : arredondar(peso.valorKg()),
                peso == null ? null : peso.descricaoOrigem(),
                altura == null ? null : arredondar(altura.emCentimetros()),
                altura == null ? null : altura.descricaoOrigem(),
                peso == null ? motivoDoPesoAusente(e) : null,

                arredondar(imc),
                AntropometriaCalculator.classificarImcOms(imc),
                AntropometriaCalculator.classificarImcOpas(imc),
                motivoImc,

                arredondar(pesoIdeal), arredondar(pesoIdeal25),
                arredondar(pesoAjustado), arredondar(pesoAmputacao),

                arredondarPercentual(perda), classifPerda, motivoPerda,

                arredondar(p50), arredondarPercentual(adequacao),
                AntropometriaCalculator.classificarAdequacaoCircBraco(adequacao),
                motivoAdequacao,

                arredondar(cbAjustada),
                AntropometriaCalculator.classificarMassaMuscularBraco(cbAjustada, e.sexo()),
                arredondar(cpAjustada),
                AntropometriaCalculator.classificarDeplecaoPanturrilha(cpAjustada, e.sexo()),
                populacao.getDescricao(),
                motivoPopulacao,
                imc != null && imc.compareTo(IMC_LIMITE_MAGREZA) < 0,
                motivoMassaBraco, motivoDeplecaoPanturrilha);
    }

    /**
     * Por que esta medida não classificou — <b>nomeando o que falta</b>.
     *
     * <p>A mensagem anterior era uma só para as duas medidas, e dizia sempre
     * "informe peso e altura": num paciente com peso e altura registrados mas
     * sem a panturrilha medida, ela culpava o dado que estava lá.
     *
     * @param ajustada  a circunferência já ajustada, ou {@code null}
     * @param medida    a circunferência crua que a alimenta
     * @param quala     "do braço" ou "da panturrilha", para a frase
     */
    private static String motivoDoAjuste(BigDecimal ajustada, BigDecimal medida,
                                         BigDecimal imc, Sexo sexo, String quala) {
        // O ajuste saiu e o sexo existe: a classificação saiu com ele.
        if (ajustada != null && sexo != null) return null;

        if (!positivo(medida))
            return "Informe a circunferência %s".formatted(quala);
        if (imc == null)
            return "O ajuste pelo IMC precisa do IMC: informe peso e altura";
        // Medida e IMC existem, então o que falta é o sexo — os cortes são
        // separados por sexo, e aplicar o do outro muda o diagnóstico.
        return "Informe o sexo: os pontos de corte são separados por sexo";
    }

    // ─── Aba 2 — Necessidades ───────────────────────────────────────────

    private static Metas necessidades(EntradaUti e, PesoDeTrabalho peso,
                                      BigDecimal imc, Altura altura) {
        if (peso == null)
            return vaziaComMotivo(motivoDoPesoAusente(e));

        boolean obeso = NecessidadeCalculator.ehObeso(imc);
        TerapiaRenal renal = e.terapiaRenalOuNenhuma();

        PesoDeTrabalho pesoIdeal = null;
        if (altura != null && e.sexo() != null) {
            BigDecimal ideal = AntropometriaCalculator.pesoIdealPorImc(
                    e.sexo() == Sexo.FEMININO ? IMC_ALVO_MULHER : IMC_ALVO_HOMEM, altura);
            if (positivo(ideal)) pesoIdeal = new PesoDeTrabalho(ideal, OrigemValor.PESO_IDEAL);
        }

        NecessidadeCalculator.Faixa energia;
        NecessidadeCalculator.Faixa proteina = null;
        BigDecimal proteinaObeso = null;
        String baseDoPeso = null;
        OrigemValor origemEnergia;
        OrigemValor origemProteina;

        if (obeso) {
            energia = NecessidadeCalculator.energiaObesidade(imc, peso, pesoIdeal);
            proteinaObeso = NecessidadeCalculator.proteinaObesidade(imc, pesoIdeal);
            origemEnergia = OrigemValor.META_OBESIDADE;
            origemProteina = OrigemValor.META_OBESIDADE;
            baseDoPeso = imc.compareTo(NecessidadeCalculator.IMC_OBESIDADE_GRAVE) >= 0
                    ? "energia sobre o peso ideal · proteína sobre o peso ideal"
                    : "energia sobre o peso atual · proteína sobre o peso ideal";
        } else {
            energia = NecessidadeCalculator.energiaPorFase(e.fase(), peso);
            proteina = NecessidadeCalculator.proteinaPorFase(e.fase(), peso);
            origemEnergia = OrigemValor.META_POR_FAIXA;
            origemProteina = OrigemValor.META_POR_FAIXA;
        }

        // A terapia renal substitui a proteína, venha ela da fase ou da obesidade
        BigDecimal proteinaRenal = NecessidadeCalculator.proteinaTerapiaRenal(renal, peso);

        // Onde na faixa fixar a meta — o topo é o padrão, e a escolha é visível
        PosicaoNaFaixa posicao = e.posicaoOuPadrao();

        // O alvo digitado vence tudo, e não passa pela posição: é valor único
        MetaEnergetica metaEnergetica = positivo(e.kcalPorKgAlvo())
                ? NecessidadeCalculator.energiaPersonalizada(e.kcalPorKgAlvo(), peso)
                : NecessidadeCalculator.metaDaFaixa(energia, origemEnergia, posicao);

        MetaProteica metaProteica;
        if (positivo(e.proteinaPorKgAlvo()))
            metaProteica = NecessidadeCalculator.proteinaPersonalizada(e.proteinaPorKgAlvo(), peso);
        else if (proteinaRenal != null)
            metaProteica = new MetaProteica(proteinaRenal, OrigemValor.META_POR_FAIXA);
        else if (proteinaObeso != null)
            metaProteica = new MetaProteica(proteinaObeso, origemProteina);
        else
            metaProteica = NecessidadeCalculator.metaProteicaDaFaixa(proteina, origemProteina, posicao);

        String motivo = null;
        if (metaEnergetica == null) motivo = SEM_FASE;
        else if (obeso && pesoIdeal == null)
            motivo = "O protocolo de obesidade usa o peso ideal: informe a altura e o sexo";

        ResultadoUti.Necessidades dto = new ResultadoUti.Necessidades(
                energia == null ? null : arredondar(energia.minimo()),
                energia == null ? null : arredondar(energia.maximo()),
                proteina == null ? arredondar(proteinaObeso) : arredondar(proteina.minimo()),
                proteina == null ? arredondar(proteinaObeso) : arredondar(proteina.maximo()),

                metaEnergetica == null ? null : arredondar(metaEnergetica.kcalDia()),
                metaEnergetica == null ? null : metaEnergetica.descricaoOrigem(),
                metaProteica == null ? null : arredondar(metaProteica.gramasDia()),
                metaProteica == null ? null : metaProteica.descricaoOrigem(),

                arredondar(proteinaRenal), obeso, baseDoPeso, motivo);

        return new Metas(dto, metaEnergetica, metaProteica);
    }

    private static Metas vaziaComMotivo(String motivo) {
        return new Metas(new ResultadoUti.Necessidades(null, null, null, null,
                null, null, null, null, null, false, null, motivo), null, null);
    }

    /**
     * As metas viajam como tipo, não como número relido do DTO.
     *
     * <p>Reconstruir {@link MetaEnergetica} a partir do {@code ResultadoUti}
     * obrigaria a inventar uma origem — e a aba da dieta passaria a exibir "da
     * faixa da fase" para uma meta que veio do protocolo de obesidade ou de um
     * alvo digitado. Perder a procedência no meio da cascata é exatamente o que
     * esta classe existe para impedir.
     */
    private record Metas(ResultadoUti.Necessidades dto,
                         MetaEnergetica energetica,
                         MetaProteica proteica) {}

    // ─── Aba 3 — Dieta enteral ──────────────────────────────────────────

    private static ResultadoUti.Dieta dieta(EntradaUti e, FormulaEnteralResolvida formula,
                                            ModuloProteicoResolvido modulo,
                                            PesoDeTrabalho peso, MetaEnergetica metaEnergetica,
                                            MetaProteica metaProteica) {
        if (formula == null)
            return dietaVazia(null, SEM_FORMULA);

        ModoInfusao modo = e.modoInfusao() == null ? ModoInfusao.CONTINUA : e.modoInfusao();
        VolumeDieta vt = DietaEnteralCalculator.volumeTotal(e.volumePorTempo(), e.tempo(), modo);

        if (vt == null)
            return dietaVazia(formula, SEM_VOLUME_OU_TEMPO);

        BigDecimal kcal = DietaEnteralCalculator.caloriasOfertadas(formula.densidadeKcalMl(), vt);
        BigDecimal ptn = DietaEnteralCalculator.proteinaOfertada(formula.proteinaGL(), vt);

        BigDecimal volumePleno = DietaEnteralCalculator.volumePleno(
                metaEnergetica, formula.densidadeKcalMl(), e.tempo());

        // A lacuna já existia e já ia para a tela. O que faltava era o passo
        // seguinte: com que produto, e quanto dele, ela se cobre.
        BigDecimal lacuna = DietaEnteralCalculator.proteinaSuplementar(metaProteica, ptn);

        DietaEnteralCalculator.SugestaoModulo sugestao = modulo == null ? null
                : DietaEnteralCalculator.moduloProteico(
                        lacuna, modulo.medidaG(),
                        modulo.proteinaPorMedidaG(), modulo.kcalPorMedida());

        List<ResultadoUti.DegrauProgressao> progressao = DietaEnteralCalculator
                .progressao(metaEnergetica, formula.densidadeKcalMl(), e.tempo())
                .stream()
                .map(d -> new ResultadoUti.DegrauProgressao(
                        d.dia(), d.pct(), arredondar(d.kcal()), arredondar(d.volume())))
                .toList();

        return new ResultadoUti.Dieta(
                formula.nome(), formula.densidadeKcalMl(), formula.proteinaGL(),

                arredondar(vt.ml()), vt.modoDescricao(),
                arredondar(kcal), arredondar(ptn),
                arredondar(DietaEnteralCalculator.caloriasPorQuilo(kcal, peso)),
                arredondar(DietaEnteralCalculator.proteinaPorQuilo(ptn, peso)),
                arredondarPercentual(DietaEnteralCalculator.percentualDoVct(kcal, metaEnergetica)),
                arredondarPercentual(DietaEnteralCalculator.percentualDaProteina(ptn, metaProteica)),

                arredondar(porLitro(formula.choGL(), vt)),
                arredondar(porLitro(formula.lipGL(), vt)),
                arredondar(porLitro(formula.fibrasGL(), vt)),
                arredondar(porLitro(formula.potassioMgL(), vt)),

                arredondar(volumePleno),
                arredondar(DietaEnteralCalculator.proteinaNoVolumePleno(
                        volumePleno, e.tempo(), formula.proteinaGL())),
                arredondar(lacuna),
                modo.getRotuloVolume(),

                modulo == null ? null : modulo.nome(),
                arredondar(sugestao == null ? null : sugestao.gramas()),
                arredondar(sugestao == null ? null : sugestao.medidas()),
                arredondar(sugestao == null ? null : sugestao.kcal()),
                motivoDoModulo(modulo, lacuna, sugestao),

                progressao,
                null,
                peso == null ? motivoDoPesoAusente(e) : null);
    }

    /**
     * Por que não há sugestão de módulo — e são três coisas diferentes.
     *
     * <p>Meta atingida é <b>boa notícia</b>, e dizer "escolha um módulo" ali
     * mandaria suplementar quem já está coberto. Sem módulo escolhido é convite.
     * Módulo escolhido sem composição é defeito de cadastro, e precisa apontar
     * para onde se conserta.
     */
    private static String motivoDoModulo(ModuloProteicoResolvido modulo, BigDecimal lacuna,
                                         DietaEnteralCalculator.SugestaoModulo sugestao) {
        if (sugestao != null) return null;
        // Sem meta não há lacuna, e sem lacuna não há sugestão — mas o bloco
        // ficava com três traços mudos e nenhuma palavra, que é exatamente o
        // que este sistema existe para não fazer.
        if (lacuna == null) return SEM_META_PROTEICA;
        if (lacuna.signum() <= 0) return META_PROTEICA_ATINGIDA;
        if (modulo == null) return SEM_MODULO;
        return MODULO_SEM_COMPOSICAO.formatted(modulo.nome());
    }

    /** Mesma conta da proteína: o catálogo é por litro, então divide por 1000. */
    private static BigDecimal porLitro(BigDecimal porLitro, VolumeDieta vt) {
        if (porLitro == null || vt == null) return null;
        return porLitro.multiply(vt.ml(), CONTA).divide(UtiMatematica.MIL, CONTA);
    }

    private static ResultadoUti.Dieta dietaVazia(FormulaEnteralResolvida formula, String motivo) {
        return new ResultadoUti.Dieta(
                formula == null ? null : formula.nome(),
                formula == null ? null : formula.densidadeKcalMl(),
                formula == null ? null : formula.proteinaGL(),
                null, null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                // Nem o módulo: sem dieta não há lacuna, e a sugestão falta
                // pela mesma razão que o bloco todo.
                null, null, null, null, null,
                // A tabela não tem motivo próprio aqui: ela falta pela mesma
                // razão que o bloco todo, e repetir a frase seria ruído.
                List.of(), null, motivo);
    }

    // ─── Aba 4 — Hidratação ─────────────────────────────────────────────

    private static ResultadoUti.Hidratacao hidratacao(EntradaUti e, FormulaEnteralResolvida formula,
                                                      PesoDeTrabalho peso, ResultadoUti.Dieta dieta) {
        if (peso == null)
            return new ResultadoUti.Hidratacao(null, null, null, null, null, null,
                    null, null, List.of(), List.of(), null, motivoDoPesoAusente(e));

        BigDecimal minima = HidratacaoCalculator.necessidadeMinima(peso);
        BigDecimal ideal = HidratacaoCalculator.necessidadeIdeal(peso);

        // O volume da dieta vem da aba anterior; o campo manual é a saída para
        // quem calcula a água sem ter preenchido a dieta.
        BigDecimal volumeMl = dieta.volumeTotalMl() != null
                ? dieta.volumeTotalMl() : e.volumeDietaManualMl();

        HidratacaoCalculator.PercentualAgua perc = formula == null ? null
                : HidratacaoCalculator.percentualDeAgua(
                        formula.aguaLivrePerc(), formula.densidadeKcalMl());

        BigDecimal naDieta = (volumeMl == null || perc == null) ? null
                : HidratacaoCalculator.aguaNaDieta(new VolumeDieta(volumeMl, ""), perc);

        BigDecimal extraMinima = HidratacaoCalculator.aguaExtra(minima, naDieta);
        BigDecimal extraIdeal = HidratacaoCalculator.aguaExtra(ideal, naDieta);

        String motivo = null;
        if (formula != null && perc == null) motivo = SEM_DENSIDADE_NA_TABELA;
        else if (volumeMl == null) motivo = "Informe a dieta, ou o volume dela, para descontar a água que ela já entrega";

        return new ResultadoUti.Hidratacao(
                arredondar(minima), arredondar(ideal),
                perc == null ? null : perc.valor(),
                perc == null ? null : perc.descricaoOrigem(),
                arredondar(volumeMl), arredondar(naDieta),
                arredondar(extraMinima), arredondar(extraIdeal),
                fracoes(extraMinima), fracoes(extraIdeal),
                null, motivo);
    }

    private static List<ResultadoUti.FracaoAgua> fracoes(BigDecimal aguaExtra) {
        List<ResultadoUti.FracaoAgua> resultado = new ArrayList<>();
        for (var fracao : HidratacaoCalculator.distribuir(aguaExtra))
            resultado.add(new ResultadoUti.FracaoAgua(
                    fracao.vezesAoDia(), arredondar(fracao.mlPorVez())));
        return List.copyOf(resultado);
    }

    // ─────────────────────────────────────────────────────────────────────
}
