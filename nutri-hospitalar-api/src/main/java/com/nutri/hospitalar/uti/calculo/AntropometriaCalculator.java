package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.calculo.cascata.Altura;
import com.nutri.hospitalar.uti.enums.JanelaPerdaPeso;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.enums.SegmentoAmputado;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CEM;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.dividir;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.percentual;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.positivo;

/**
 * Diagnóstico antropométrico: altura estimada, IMC e sua classificação, metas de
 * peso, perda de peso e depleção muscular.
 *
 * <p>Especificação: {@code docs/10-calculos-uti-adulto.md} §2. Divergiu da
 * planilha, este código está errado — não o contrário. As estimativas de
 * <i>peso</i> ficam em {@link EstimativaPesoCalculator}.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco. Medidas em centímetros,
 * peso em quilos, sem arredondamento — quem arredonda é a saída.
 *
 * <p>Duas coisas aqui <b>não existem em célula nenhuma da planilha</b>: o ajuste
 * da circunferência da panturrilha e do braço pelo IMC. Eles só aparecem em duas
 * imagens ancoradas na aba, e é por isso que o eroERP nunca os implementou —
 * ver {@link #ajustarCircPanturrilha} e {@link #ajustarCircBraco}.
 */
public final class AntropometriaCalculator {

    private AntropometriaCalculator() {}

    // ─── Altura estimada — Chumlea 1985 ─────────────────────────────────

    /**
     * Altura estimada pela altura do joelho — Chumlea et al. (1985), para quem
     * não pode ser medido de pé.
     *
     * <pre>
     * Homem : 64,19 − (0,04 × idade) + (2,02 × AJ)
     * Mulher: 84,88 − (0,24 × idade) + (1,83 × AJ)
     * </pre>
     *
     * <p>Confere com {@code Estimativas!B9}/{@code B10}: AJ 53 cm, 59 anos →
     * 168,89 (H) e 167,71 (M).
     *
     * @return altura em <b>centímetros</b>, ou {@code null} se faltar entrada
     */
    public static BigDecimal alturaChumlea1985(BigDecimal alturaJoelhoCm,
                                               Integer idadeAnos, Sexo sexo) {
        if (alturaJoelhoCm == null || idadeAnos == null || sexo == null) return null;

        String[] c = sexo == Sexo.MASCULINO
                ? new String[]{"64.19", "0.04", "2.02"}
                : new String[]{"84.88", "0.24", "1.83"};

        return new BigDecimal(c[0])
                .subtract(new BigDecimal(idadeAnos).multiply(new BigDecimal(c[1]), CONTA), CONTA)
                .add(alturaJoelhoCm.multiply(new BigDecimal(c[2]), CONTA), CONTA);
    }

    // ─── IMC e classificação ────────────────────────────────────────────

    /** {@code peso / altura²}. Confere: (62 kg; 1,75 m) → 20,2449. */
    public static BigDecimal imc(BigDecimal pesoKg, Altura altura) {
        if (!positivo(pesoKg) || altura == null) return null;
        return dividir(pesoKg, altura.aoQuadrado());
    }

    /**
     * Classificação de IMC da <b>OMS 1997</b> ({@code Estimativas!A43:B49}), em
     * seis faixas.
     *
     * <p><b>Cada faixa é fechada à esquerda.</b> Os rótulos da planilha são
     * "Sobrepeso &gt;25 &lt;30" e "Obesidade Grau I &gt;30 a 34,9" — com eles,
     * IMC 25,0 e 30,0 exatos não caem em faixa nenhuma (defeito 17 de
     * {@code docs/10} §11). Aqui caem, e há teste nas nove fronteiras.
     */
    public static Classificacao classificarImcOms(BigDecimal imc) {
        if (imc == null) return null;
        if (menorQue(imc, "18.5")) return Classificacao.critica("Desnutrição");
        if (menorQue(imc, "25"))   return Classificacao.adequada("Eutrofia");
        if (menorQue(imc, "30"))   return Classificacao.atencao("Sobrepeso");
        if (menorQue(imc, "35"))   return Classificacao.atencao("Obesidade grau I");
        if (menorQue(imc, "40"))   return Classificacao.critica("Obesidade grau II");
        return Classificacao.critica("Obesidade grau III");
    }

    /**
     * Classificação de IMC da <b>OPAS 2002</b> para o idoso
     * ({@code Estimativas!C43:C47}), em <b>quatro</b> faixas.
     *
     * <p>Derivada do estudo SABE, sete países da América Latina e Caribe. A
     * planilha tabula as quatro; o eroERP implementou só dois cortes
     * ({@code <23} · {@code ≤28} · resto) e <b>perde a distinção entre excesso
     * de peso e obesidade no idoso</b>. Ver {@code docs/10} §2.6.
     */
    public static Classificacao classificarImcOpas(BigDecimal imc) {
        if (imc == null) return null;
        if (menorQue(imc, "23")) return Classificacao.critica("Baixo peso");
        if (menorQue(imc, "28")) return Classificacao.adequada("Eutrofia");
        if (menorQue(imc, "30")) return Classificacao.atencao("Excesso de peso");
        return Classificacao.atencao("Obesidade");
    }

    // ─── Metas de peso ──────────────────────────────────────────────────

    /**
     * Peso ideal para um IMC alvo: {@code IMC_alvo × altura²}.
     *
     * <p>Alvos usados pela planilha ({@code Estimativas!E4:E6}): 22 para homem,
     * 20,8 para mulher e 25 como referência superior. Confere, altura 1,68 m →
     * 62,0928 · 58,70592 · 70,56.
     */
    public static BigDecimal pesoIdealPorImc(BigDecimal imcAlvo, Altura altura) {
        if (imcAlvo == null || altura == null) return null;
        return imcAlvo.multiply(altura.aoQuadrado(), CONTA);
    }

    /**
     * Peso ajustado: {@code (atual − ideal) × 0,33 + ideal}
     * ({@code Estimativas!E9}).
     *
     * <p><b>De onde vem o 0,33.</b> É a fração metabolicamente ativa do excesso
     * de peso: cerca de <b>25 % do tecido adiposo é massa magra</b>, e o
     * restante do excesso é gordura, que consome pouca energia. A prática
     * clínica arredonda para 0,25 ou 0,33 conforme a escola — Wilkens (1986) e a
     * ADA popularizaram o 0,25; o 0,33 é a variante conservadora, e é a que a
     * planilha usa. Não é constante com fórmula fechada: é convenção clínica, e
     * este sistema segue a da planilha porque é a que a Silvia pratica.
     *
     * <p><b>Só faz sentido em excesso de peso.</b> Com atual abaixo do ideal a
     * expressão devolve valor <i>entre</i> os dois, o que não é o propósito —
     * quem decide se o ajustado entra na cascata é
     * {@code PesoDeTrabalho}, e ele só o oferece quando há obesidade.
     *
     * <p>Confere: (85; 58,71) → 67,3857.
     */
    public static BigDecimal pesoAjustado(BigDecimal pesoAtualKg, BigDecimal pesoIdealKg) {
        if (pesoAtualKg == null || pesoIdealKg == null) return null;
        return pesoAtualKg.subtract(pesoIdealKg, CONTA)
                .multiply(new BigDecimal("0.33"), CONTA)
                .add(pesoIdealKg, CONTA);
    }

    /**
     * Peso corrigido por amputação: desconta a soma dos percentuais dos
     * segmentos ausentes ({@code Estimativas!G29}, Osterkamp 1995).
     *
     * <p>Confere: (65 kg; 1,6 %) → 63,96.
     *
     * <p><b>Só chame depois de {@link #conflitoDeSegmentos} devolver vazio.</b>
     */
    public static BigDecimal pesoCorrigidoPorAmputacao(BigDecimal pesoKg,
                                                       Set<SegmentoAmputado> segmentos) {
        if (!positivo(pesoKg)) return null;
        if (segmentos == null || segmentos.isEmpty()) return pesoKg;

        BigDecimal percentualTotal = segmentos.stream()
                .map(SegmentoAmputado::getPercentual)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b, CONTA));

        return pesoKg.subtract(
                pesoKg.multiply(percentualTotal, CONTA).divide(CEM, CONTA), CONTA);
    }

    /**
     * A combinação de segmentos desconta o mesmo pedaço duas vezes?
     *
     * <p>Os segmentos de Osterkamp <b>se contêm</b>: a própria tabela mostra
     * 0,7 + 1,6 = 2,3 e 2,7 + 2,3 = 5,0. Escolher "Membro superior" e "Mão"
     * junto desconta a mão duas vezes e devolve um peso menor que o real — em
     * silêncio, que é o pior desfecho possível para uma prescrição.
     *
     * @return a mensagem nomeando os dois segmentos sobrepostos, ou vazio se a
     *         combinação é coerente. Quem transforma isso em 422 é o service:
     *         esta classe não conhece HTTP.
     */
    public static Optional<String> conflitoDeSegmentos(Set<SegmentoAmputado> segmentos) {
        if (segmentos == null || segmentos.size() < 2) return Optional.empty();

        List<SegmentoAmputado> ordenados = segmentos.stream()
                .sorted(Comparator.comparing(SegmentoAmputado::getPercentual).reversed())
                .toList();

        for (SegmentoAmputado maior : ordenados)
            for (SegmentoAmputado menor : ordenados)
                if (maior != menor && maior.contem(menor))
                    return Optional.of(
                            "\"%s\" já inclui \"%s\" — marcar os dois desconta o mesmo segmento duas vezes. Escolha apenas um."
                                    .formatted(maior.getDescricao(), menor.getDescricao()));

        return Optional.empty();
    }

    // ─── Perda de peso — Blackburn 1977 ─────────────────────────────────

    /**
     * {@code (usual − atual) × 100 / usual} ({@code Estimativas!B32}).
     *
     * <p>Confere: (68 usual; 60 atual) → 11,7647 %. Ganho de peso devolve
     * percentual negativo, e a classificação trata disso.
     */
    public static BigDecimal percentualPerdaPeso(BigDecimal pesoUsualKg, BigDecimal pesoAtualKg) {
        if (!positivo(pesoUsualKg) || !positivo(pesoAtualKg)) return null;
        return percentual(pesoUsualKg.subtract(pesoAtualKg, CONTA), pesoUsualKg);
    }

    /**
     * Classifica a perda <b>pela janela de tempo</b> — Blackburn 1977.
     *
     * <p>O mesmo percentual muda de diagnóstico conforme o tempo: 5 % em uma
     * semana é grave, 5 % em seis meses é moderado. Ver {@link JanelaPerdaPeso}.
     */
    public static Classificacao classificarPerdaPeso(BigDecimal percentualPerda,
                                                     JanelaPerdaPeso janela) {
        if (percentualPerda == null || janela == null) return null;

        if (percentualPerda.signum() <= 0)
            return Classificacao.adequada("Sem perda de peso");

        if (percentualPerda.compareTo(janela.getPerdaGrave()) >= 0)
            return Classificacao.critica(
                    "Perda grave em %s".formatted(janela.getDescricao()));

        if (percentualPerda.compareTo(janela.getPerdaModeradaMin()) >= 0)
            return Classificacao.atencao(
                    "Perda moderada em %s".formatted(janela.getDescricao()));

        return Classificacao.adequada(
                "Perda abaixo do corte de %s".formatted(janela.getDescricao()));
    }

    // ─── Adequação da circunferência do braço ───────────────────────────

    /**
     * {@code CB_medida / CB_P50 × 100} ({@code Estimativas!J25}).
     *
     * <p>Confere: (30; 32,3) → 92,8793 % → Eutrofia.
     *
     * <p>O P50 vem da tabela por sexo e faixa etária, <b>buscado no banco</b>
     * (migration 020) — na planilha ele é digitado à mão em {@code J24}, que é o
     * defeito 19. Fora de 18 a 90,9 anos não há linha, e o resultado é ausência
     * com motivo: o eroERP extrapola em silêncio usando a linha do extremo.
     */
    public static BigDecimal adequacaoCircBraco(BigDecimal circBracoCm, BigDecimal p50Cm) {
        return percentual(circBracoCm, p50Cm);
    }

    /**
     * Classificação da % de adequação de CB, em <b>seis faixas</b>
     * ({@code Estimativas!N12:O17}).
     *
     * <p><b>Os cortes — 70 · 80 · 90 · 110 · 120 % — são de Blackburn e Thornton
     * (1979)</b>, o esquema clássico de adequação antropométrica contra o
     * percentil 50 de referência: abaixo de 90 % há déficit em três graus, entre
     * 90 e 110 % é eutrofia, e acima há excesso em dois graus. É a mesma régua
     * que a planilha tabula, e vale para CB, CMB e prega tricipital.
     *
     * <p>A <b>tabela de percentis</b> contra a qual se compara é outra coisa e
     * tem fonte própria — ver {@code PercentilCb} e {@code docs/10 §2.8}.
     */
    public static Classificacao classificarAdequacaoCircBraco(BigDecimal adequacaoPerc) {
        if (adequacaoPerc == null) return null;
        if (menorQue(adequacaoPerc, "70"))  return Classificacao.critica("Desnutrição grave");
        if (menorQue(adequacaoPerc, "80"))  return Classificacao.critica("Desnutrição moderada");
        if (menorQue(adequacaoPerc, "90"))  return Classificacao.atencao("Desnutrição leve");
        if (menorQue(adequacaoPerc, "110")) return Classificacao.adequada("Eutrofia");
        if (menorQue(adequacaoPerc, "120")) return Classificacao.atencao("Sobrepeso");
        return Classificacao.atencao("Obesidade");
    }

    // ─── Depleção muscular, com o ajuste pelo IMC ───────────────────────

    /**
     * Ajusta a circunferência da panturrilha pelo IMC, <b>antes</b> de comparar
     * com o ponto de corte.
     *
     * <p><b>Este passo não existe em célula nenhuma da planilha</b> — está só na
     * imagem {@code image3.jpeg}, ancorada em {@code D33} — e por isso o eroERP
     * não o faz. A diferença inverte o resultado no paciente obeso: CP medida de
     * 36 cm com IMC 32 sai "adequada" sem o ajuste (36 &gt; 34); com o desconto
     * de 7 cm da faixa, a CP ajustada é 29 cm → <b>depleção muscular</b>.
     *
     * <p>Fonte: Gonzalez MC, Mehrnezhad A, Razaviarab N, Barbosa-Silva TG,
     * Heymsfield SB. "Calf circumference: cutoff values from the NHANES
     * 1999–2006." <i>Am J Clin Nutr</i> 2021;113(6):1679-87, Tabelas 5 e 6.
     *
     * <p>O acréscimo em IMC &lt; 18,5 só se aplica à coluna do adulto saudável —
     * ver {@link PopulacaoReferencia}.
     */
    public static BigDecimal ajustarCircPanturrilha(BigDecimal circPanturrilhaCm, BigDecimal imc,
                                                    PopulacaoReferencia populacao) {
        if (circPanturrilhaCm == null || imc == null || populacao == null) return null;

        String ajuste;
        if (menorQue(imc, "18.5"))    ajuste = populacao.somaNoMagro() ? "4" : "0";
        else if (menorQue(imc, "25")) ajuste = "0";
        else if (menorQue(imc, "30")) ajuste = "-3";
        else if (menorQue(imc, "40")) ajuste = "-7";
        else                          ajuste = "-12";

        return circPanturrilhaCm.add(new BigDecimal(ajuste), CONTA);
    }

    /**
     * Corte da CP <b>já ajustada</b>: homem &lt; 34 cm, mulher &lt; 33 cm
     * ({@code Estimativas!I28:J30}, Barbosa-Silva 2016).
     */
    public static Classificacao classificarDeplecaoPanturrilha(BigDecimal circAjustadaCm, Sexo sexo) {
        if (circAjustadaCm == null || sexo == null) return null;

        BigDecimal corte = sexo == Sexo.MASCULINO ? new BigDecimal("34") : new BigDecimal("33");

        return circAjustadaCm.compareTo(corte) < 0
                ? Classificacao.critica("Indicativo de depleção muscular")
                : Classificacao.adequada("Sem indicativo de depleção");
    }

    /**
     * Ajusta a circunferência do braço pelo IMC, <b>antes</b> de comparar com o
     * corte. Aqui o ajuste <b>depende do sexo</b>.
     *
     * <p>Como o da panturrilha, só existe na imagem — {@code image4.png},
     * ancorada em {@code J33}.
     *
     * <p>Fonte: Costa-Pereira JP, Prado CM, Heymsfield SB, Fayh APT,
     * Menezes-Júnior LAA, Cabral PC, Diniz AS, Gonzalez MC. "Arm circumference
     * as a marker of muscle mass: cutoff values from NHANES 1999–2006."
     * <i>Am J Clin Nutr</i> 2025;122(6):1809-18.
     */
    public static BigDecimal ajustarCircBraco(BigDecimal circBracoCm, BigDecimal imc,
                                              Sexo sexo, PopulacaoReferencia populacao) {
        if (circBracoCm == null || imc == null || sexo == null || populacao == null) return null;

        boolean homem = sexo == Sexo.MASCULINO;

        String ajuste;
        if (menorQue(imc, "18.5"))    ajuste = populacao.somaNoMagro() ? (homem ? "3" : "2") : "0";
        else if (menorQue(imc, "25")) ajuste = "0";
        else if (menorQue(imc, "30")) ajuste = homem ? "-3"  : "-2";
        else if (menorQue(imc, "40")) ajuste = homem ? "-7"  : "-6";
        else                          ajuste = homem ? "-10" : "-9";

        return circBracoCm.add(new BigDecimal(ajuste), CONTA);
    }

    /**
     * Cortes da CB <b>já ajustada</b>, em <b>dois</b> níveis: homem &lt; 28 cm
     * (baixa) e &lt; 26 cm (muito baixa); mulher &lt; 25 cm e &lt; 23 cm.
     *
     * <p>As células da planilha ({@code L23:M25}) trazem só o nível "baixa" — o
     * "muito baixa" existe apenas na imagem.
     */
    public static Classificacao classificarMassaMuscularBraco(BigDecimal circAjustadaCm, Sexo sexo) {
        if (circAjustadaCm == null || sexo == null) return null;

        boolean homem = sexo == Sexo.MASCULINO;
        BigDecimal muitoBaixa = new BigDecimal(homem ? "26" : "23");
        BigDecimal baixa      = new BigDecimal(homem ? "28" : "25");

        if (circAjustadaCm.compareTo(muitoBaixa) < 0)
            return Classificacao.critica("Massa muscular muito baixa");
        if (circAjustadaCm.compareTo(baixa) < 0)
            return Classificacao.atencao("Massa muscular baixa");
        return Classificacao.adequada("Massa muscular adequada");
    }

    // ─────────────────────────────────────────────────────────────────────

    /** Faixa fechada à esquerda: o valor exato do corte pertence à faixa de cima. */
    private static boolean menorQue(BigDecimal valor, String corte) {
        return valor.compareTo(new BigDecimal(corte)) < 0;
    }
}
