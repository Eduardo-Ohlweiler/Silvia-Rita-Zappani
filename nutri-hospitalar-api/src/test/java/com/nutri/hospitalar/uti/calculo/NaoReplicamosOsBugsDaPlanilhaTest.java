package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.calculo.cascata.Altura;
import com.nutri.hospitalar.uti.calculo.cascata.MetaEnergetica;
import com.nutri.hospitalar.uti.calculo.cascata.MetaProteica;
import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;
import com.nutri.hospitalar.uti.calculo.cascata.VolumeDieta;
import com.nutri.hospitalar.uti.enums.EtniaChumlea;
import com.nutri.hospitalar.uti.enums.ModoInfusao;
import com.nutri.hospitalar.uti.enums.OrigemValor;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.enums.SegmentoAmputado;
import com.nutri.hospitalar.uti.enums.TomResultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Um teste por defeito de {@code docs/10} §11.
 *
 * <p><b>Nenhum deles confere o número da planilha.</b> Todos verificam que o
 * nosso é <i>diferente</i> do dela, e por quê. É o único formato que impede a
 * regressão: um teste que afirmasse o valor correto passaria a passar de novo se
 * alguém "consertasse" o código de volta para o comportamento da planilha, desde
 * que o valor correto coincidisse por acaso.
 *
 * <p>Os defeitos 1, 2 e 19 são travados no banco e vivem em
 * {@code CatalogoUtiTest}; o 22 está fora do escopo desta fatia.
 */
@DisplayName("Não replicamos os defeitos da planilha")
class NaoReplicamosOsBugsDaPlanilhaTest {

    private final PesoDeTrabalho peso68 = PesoDeTrabalho.informado(new BigDecimal("68"));

    @Test
    @DisplayName("defeito 1 — a proteína é sempre por litro, nunca por frasco de 500 ml")
    void defeito01_proteinaSemprePorLitro() {
        // A aba Contínuo divide por 1000 a composição de um produto declarado
        // por frasco de 500 ml; a Intermitente divide por 500. Aqui o catálogo é
        // normalizado e a divisão é uma só.
        VolumeDieta vt = new VolumeDieta(new BigDecimal("1000"), "1000 ml");
        BigDecimal ptnPorLitro = new BigDecimal("100");

        BigDecimal nosso = DietaEnteralCalculator.proteinaOfertada(ptnPorLitro, vt);
        BigDecimal comoAPlanilhaFariaSeFosse500 = new BigDecimal("50");

        assertThat(nosso).isEqualByComparingTo("100");
        assertThat(nosso).isNotEqualByComparingTo(comoAPlanilhaFariaSeFosse500);
    }

    @Test
    @DisplayName("defeito 3 — o %VCT divide pelo VCT, e o tipo impede trocar pela proteína")
    void defeito03_percentualDoVctNaoUsaProteina() {
        // 11 das 52 linhas da planilha dividem o %VCT pela PTN total.
        BigDecimal kcal = new BigDecimal("1364");
        MetaEnergetica meta = new MetaEnergetica(new BigDecimal("1360"), OrigemValor.META_POR_FAIXA);
        BigDecimal proteinaTotal = new BigDecimal("125.488");

        BigDecimal nosso = DietaEnteralCalculator.percentualDoVct(kcal, meta);
        BigDecimal comoAPlanilhaFaz = UtiMatematica.percentual(kcal, proteinaTotal);

        assertThat(arredondar(nosso)).isEqualByComparingTo("100.2941");
        assertThat(arredondar(nosso)).isNotEqualByComparingTo(arredondar(comoAPlanilhaFaz));

        // E o tipo é o que torna a troca impossível de escrever: percentualDoVct
        // só aceita MetaEnergetica, e percentualDaProteina só aceita MetaProteica.
        MetaProteica metaPtn = new MetaProteica(new BigDecimal("102"), OrigemValor.META_POR_FAIXA);
        assertThat(arredondar(DietaEnteralCalculator.percentualDaProteina(proteinaTotal, metaPtn)))
                .isEqualByComparingTo("123.0275");
    }

    @Test
    @DisplayName("defeitos 4 e 5 — kcal/kg divide pelo peso, nunca pela altura nem pelo VCT")
    void defeito04e05_kcalPorQuiloUsaOPeso() {
        // Contínuo!H52 e I52 dividem pela altura; Intermitente!H5 e H6 pelo VCT.
        BigDecimal kcal = new BigDecimal("1364");
        Altura altura = Altura.informadaEmCentimetros(new BigDecimal("168"));

        BigDecimal nosso = DietaEnteralCalculator.caloriasPorQuilo(kcal, peso68);
        BigDecimal comoAPlanilhaFaz = UtiMatematica.dividir(kcal, altura.valorM());

        assertThat(arredondar(nosso)).isEqualByComparingTo("20.0588");
        assertThat(arredondar(nosso)).isNotEqualByComparingTo(arredondar(comoAPlanilhaFaz));

        // A assinatura só aceita PesoDeTrabalho — a altura nem compila aqui.
        assertThat(nosso).isLessThan(comoAPlanilhaFaz);
    }

    @Test
    @DisplayName("defeitos 6 e 7 — tempo ausente devolve ausência, não VT zero")
    void defeito06e07_semTempoNaoHaVolume() {
        // Contínuo!E38 e E52 multiplicam por célula vazia e devolvem VT = 0 —
        // dieta zerada apresentada como resultado válido. E 42 das 52 linhas têm
        // o literal 0 no lugar da fórmula.
        assertThat(DietaEnteralCalculator.volumeTotal(
                new BigDecimal("62"), null, ModoInfusao.CONTINUA)).isNull();

        assertThat(DietaEnteralCalculator.volumeTotal(
                new BigDecimal("62"), BigDecimal.ZERO, ModoInfusao.CONTINUA)).isNull();

        // Ausência não é zero: zero seria "a dieta entrega nada", que é
        // afirmação clínica. Nulo é "não sei", e a tela mostra o motivo.
    }

    @Test
    @DisplayName("defeito 8 — a lacuna proteica exige a meta, e nunca é negativa")
    void defeito08_lacunaProteica() {
        // Intermitente!N11 usa célula vazia como meta e devolve a proteína
        // inteira como lacuna.
        MetaProteica meta = new MetaProteica(new BigDecimal("102"), OrigemValor.META_POR_FAIXA);

        // Meta já superada devolve zero, não número negativo
        assertThat(DietaEnteralCalculator.proteinaSuplementar(meta, new BigDecimal("125.488")))
                .isEqualByComparingTo("0");

        // E a lacuna real é a diferença, não a proteína toda
        BigDecimal lacuna = DietaEnteralCalculator.proteinaSuplementar(meta, new BigDecimal("60"));
        assertThat(lacuna).isEqualByComparingTo("42");
        assertThat(lacuna).isNotEqualByComparingTo(new BigDecimal("60"));
    }

    @Test
    @DisplayName("defeito 9 — a água da dieta desconta de verdade da água extra")
    void defeito09_aguaDaDietaDesconta() {
        // Hidratação!D21 e E21 auto-referenciam uma célula vazia da própria aba
        // quando deveriam ler o volume da dieta artesanal: a água extra não
        // desconta nada.
        PesoDeTrabalho peso72 = PesoDeTrabalho.informado(new BigDecimal("72"));
        BigDecimal necessidade = HidratacaoCalculator.necessidadeIdeal(peso72);

        VolumeDieta dieta = new VolumeDieta(new BigDecimal("1600"), "1600 ml");
        var perc = HidratacaoCalculator.percentualDeAgua(null, new BigDecimal("2.0"));
        BigDecimal naDieta = HidratacaoCalculator.aguaNaDieta(dieta, perc);

        BigDecimal nosso = HidratacaoCalculator.aguaExtra(necessidade, naDieta);

        assertThat(nosso).isEqualByComparingTo("1040");
        // A planilha devolveria a necessidade inteira, sem descontar
        assertThat(nosso).isNotEqualByComparingTo(necessidade);
    }

    @Test
    @DisplayName("defeito 10 — a kcal do módulo vem da composição, não de gramas somadas a kcal")
    void defeito10_kcalDoModulo() {
        // Contínuo!U24 faz `lacuna×4 + gramas×9,66/20`: o segundo termo são
        // GRAMAS de carboidrato somadas a um total de KCAL, sem multiplicar por 4.
        // Nutridrink Protein: medida 20 g, 6 g PTN, 9,66 g CHO, 62,64 kcal.
        var sugestao = DietaEnteralCalculator.moduloProteico(
                new BigDecimal("45"), new BigDecimal("20"),
                new BigDecimal("6"), new BigDecimal("62.64"));

        assertThat(arredondar(sugestao.gramas())).isEqualByComparingTo("150.0000");
        assertThat(arredondar(sugestao.kcal())).isEqualByComparingTo("469.8000");

        // O número da planilha, que não reproduzimos
        BigDecimal comoAPlanilhaFaz = new BigDecimal("252.45");
        assertThat(arredondar(sugestao.kcal())).isNotEqualByComparingTo(comoAPlanilhaFaz);

        // E o mesmo cálculo num módulo SEM carboidrato concorda com a planilha —
        // é a prova aritmética de que o erro é do termo do carboidrato.
        var semCarboidrato = DietaEnteralCalculator.moduloProteico(
                new BigDecimal("45"), new BigDecimal("15"),
                new BigDecimal("13"), new BigDecimal("52"));
        assertThat(arredondar(semCarboidrato.kcal())).isEqualByComparingTo("180.0000");
    }

    @Test
    @DisplayName("defeito 11 — as embalagens usam a medida declarada no catálogo")
    void defeito11_embalagemDoOleoUsaAMedidaCerta() {
        // TNE SA!B20 conta o óleo com 10 ml onde a própria tabela declara 13 ml.
        var resultado = artesanal();

        BigDecimal nosso = resultado.embalagensPorMes().stream()
                .filter(e -> e.produto().contains("Óleo"))
                .findFirst().orElseThrow().quantidade();

        // 0,5 colher × 13 ml / 900 ml × 31 dias
        assertThat(arredondar(nosso)).isEqualByComparingTo("0.2239");

        // Com os 10 ml da planilha daria outro número
        BigDecimal comoAPlanilhaFaz = new BigDecimal("0.5")
                .multiply(new BigDecimal("10"))
                .divide(new BigDecimal("900"), UtiMatematica.CONTA)
                .multiply(new BigDecimal("31"));
        assertThat(arredondar(nosso)).isNotEqualByComparingTo(arredondar(comoAPlanilhaFaz));
    }

    @Test
    @DisplayName("defeito 12 — a receita por dose acompanha a entrada, não é constante")
    void defeito12_receitaPorDoseNaoEhConstante() {
        // TNE SA!B27 é a constante 13/4: a receita de óleo por dose não muda
        // quando se muda a quantidade de óleo.
        BigDecimal comMeiaColher = oleoPorAdministracao(new BigDecimal("0.5"));
        BigDecimal comDuasColheres = oleoPorAdministracao(new BigDecimal("2"));

        assertThat(comMeiaColher).isEqualByComparingTo("0.125");   // 0,5 / 4
        assertThat(comDuasColheres).isEqualByComparingTo("0.5");   // 2 / 4

        // Na planilha os dois dariam o mesmo número
        assertThat(comMeiaColher).isNotEqualByComparingTo(comDuasColheres);
    }

    @Test
    @DisplayName("defeito 13 — os dois denominadores de percentual aparecem nomeados")
    void defeito13_doisDenominadores() {
        // TNE SA!H9:J9 divide pelo VET desejado e os três somam 97,33 % sem
        // dizer. Mostramos os dois, e o que soma 100 existe.
        var resultado = artesanal();

        BigDecimal somaSobreVet = resultado.percChoSobreVet()
                .add(resultado.percPtnSobreVet()).add(resultado.percLipSobreVet());
        BigDecimal somaSobreOfertado = resultado.percChoSobreOfertado()
                .add(resultado.percPtnSobreOfertado()).add(resultado.percLipSobreOfertado());

        assertThat(somaSobreVet.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo("97.33");
        assertThat(somaSobreOfertado.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("defeitos 14 e 15 — o peso e a meta viajam com a origem colada")
    void defeito14e15_acascataEhTipada() {
        // A planilha tem o peso em 8 células independentes e a meta redigitada em
        // cada aba. Aqui um peso sem origem não existe: o construtor recusa.
        PesoDeTrabalho estimado = new PesoDeTrabalho(
                new BigDecimal("66.3083"), OrigemValor.ESTIMADO_RABITO);

        assertThat(estimado.descricaoOrigem()).isEqualTo("peso estimado · Rabito 2008");

        // E não há construtor que aceite só o número
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PesoDeTrabalho(new BigDecimal("70"), null)))
                .hasMessageContaining("origem");

        // Peso zerado não vira peso de trabalho — é o ramo em que o eroERP
        // calcula o IMC por Chumlea e a meta calórica com 0, na mesma tela.
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PesoDeTrabalho.informado(BigDecimal.ZERO)))
                .hasMessageContaining("maior que zero");
    }

    @Test
    @DisplayName("defeito 16 — os 60 anos exatos caem na faixa do idoso")
    void defeito16_fronteiraDos60Anos() {
        // A planilha rotula "19-59 ANOS" e ">60 ANOS": os 60 exatos ficam sem
        // faixa. Convenção nossa: ≥60 é idoso (Lei 10.741/2003).
        BigDecimal aj = new BigDecimal("53");
        BigDecimal cb = new BigDecimal("25");

        BigDecimal aos59 = EstimativaPesoCalculator.chumlea1988(
                aj, cb, 59, Sexo.MASCULINO, EtniaChumlea.BRANCA);
        BigDecimal aos60 = EstimativaPesoCalculator.chumlea1988(
                aj, cb, 60, Sexo.MASCULINO, EtniaChumlea.BRANCA);
        BigDecimal aos61 = EstimativaPesoCalculator.chumlea1988(
                aj, cb, 61, Sexo.MASCULINO, EtniaChumlea.BRANCA);

        assertThat(arredondar(aos59)).isEqualByComparingTo("54.7500");
        assertThat(arredondar(aos60)).isEqualByComparingTo("59.2400");
        assertThat(aos60).isEqualByComparingTo(aos61);
        assertThat(aos60).isNotEqualByComparingTo(aos59);
    }

    @Test
    @DisplayName("defeito 17 — IMC 25,0 e 30,0 exatos caem em faixa")
    void defeito17_fronteirasDoImc() {
        // Os rótulos "Sobrepeso >25 <30" e "Obesidade I >30 a 34,9" deixam os
        // valores exatos fora. Aqui cada faixa é fechada à esquerda.
        assertThat(AntropometriaCalculator.classificarImcOms(new BigDecimal("18.5")).rotulo())
                .isEqualTo("Eutrofia");
        assertThat(AntropometriaCalculator.classificarImcOms(new BigDecimal("25.0")).rotulo())
                .isEqualTo("Sobrepeso");
        assertThat(AntropometriaCalculator.classificarImcOms(new BigDecimal("30.0")).rotulo())
                .isEqualTo("Obesidade grau I");
        assertThat(AntropometriaCalculator.classificarImcOms(new BigDecimal("35.0")).rotulo())
                .isEqualTo("Obesidade grau II");
        assertThat(AntropometriaCalculator.classificarImcOms(new BigDecimal("40.0")).rotulo())
                .isEqualTo("Obesidade grau III");

        // E a OPAS tem QUATRO faixas — o eroERP implementou só dois cortes e
        // perde a distinção entre excesso de peso e obesidade no idoso.
        assertThat(AntropometriaCalculator.classificarImcOpas(new BigDecimal("22.9")).rotulo())
                .isEqualTo("Baixo peso");
        assertThat(AntropometriaCalculator.classificarImcOpas(new BigDecimal("28.0")).rotulo())
                .isEqualTo("Excesso de peso");
        assertThat(AntropometriaCalculator.classificarImcOpas(new BigDecimal("30.0")).rotulo())
                .isEqualTo("Obesidade");
    }

    @Test
    @DisplayName("defeito 18 — proteína sobe em IMC 40, energia troca de peso em IMC 50")
    void defeito18_fronteirasDaObesidade() {
        // O cabeçalho da coluna C diz IMC>40 e o rodapé diz IMC >50. Os dois
        // rótulos NÃO se contradizem: pertencem a LINHAS diferentes da mesma
        // coluna. ASPEN/SCCM 2016 diz qual é qual — 2,5 g/kg de peso ideal a
        // partir de IMC 40, e 22–25 kcal/kg de peso ideal só acima de 50.
        // Ver docs/10 §3.3.
        PesoDeTrabalho atual = PesoDeTrabalho.informado(new BigDecimal("68"));
        PesoDeTrabalho ideal = PesoDeTrabalho.informado(new BigDecimal("82"));

        // Energia: até 50 usa peso atual, de 50 em diante usa o ideal
        assertThat(NecessidadeCalculator.energiaObesidade(new BigDecimal("49.9"), atual, ideal).maximo())
                .isEqualByComparingTo("952");
        assertThat(NecessidadeCalculator.energiaObesidade(new BigDecimal("50"), atual, ideal).maximo())
                .isEqualByComparingTo("2050");

        // Proteína: 2,0 g/kg até 40, e 2,5 daí em diante
        assertThat(NecessidadeCalculator.proteinaObesidade(new BigDecimal("39.9"), ideal))
                .isEqualByComparingTo("164");
        assertThat(NecessidadeCalculator.proteinaObesidade(new BigDecimal("40"), ideal))
                .isEqualByComparingTo("205");

        // E a trava contra a regressão: no IMC 45 a leitura invertida — a que
        // este sistema já teve — dava 2050 kcal e 164 g. A correta dá o oposto:
        // menos energia e mais proteína, que é o que a diretriz insiste.
        assertThat(NecessidadeCalculator.energiaObesidade(new BigDecimal("45"), atual, ideal).maximo())
                .isNotEqualByComparingTo("2050")
                .isEqualByComparingTo("952");
        assertThat(NecessidadeCalculator.proteinaObesidade(new BigDecimal("45"), ideal))
                .isNotEqualByComparingTo("164")
                .isEqualByComparingTo("205");
    }

    @Test
    @DisplayName("defeito 21 — densidade sem linha na tabela devolve ausência, não degrau inventado")
    void defeito21_densidadeIntermediaria() {
        // A tabela de % de água tem quatro densidades exatas; 12 produtos do
        // catálogo têm densidade intermediária. Aplicar função escada seria
        // inferência nossa sobre prescrição de UTI.
        for (String densidade : new String[]{"1.12", "1.14", "1.21", "1.24", "1.3", "1.31", "1.33"})
            assertThat(HidratacaoCalculator.percentualDeAgua(null, new BigDecimal(densidade)))
                    .as("densidade %s", densidade)
                    .isNull();

        // Mas com o dado do rótulo o cálculo acontece, e diz de onde veio
        var comRotulo = HidratacaoCalculator.percentualDeAgua(
                new BigDecimal("81"), new BigDecimal("1.24"));
        assertThat(comRotulo.origem()).isEqualTo(OrigemValor.AGUA_LIVRE_ROTULO);
    }

    @Test
    @DisplayName("fora da planilha — o ajuste de CB e CP pelo IMC, que o eroERP não faz")
    void ajustePeloImcQueSoExisteNaImagem() {
        // O caso que inverte o diagnóstico: CP de 36 cm com IMC 32.
        BigDecimal cpMedida = new BigDecimal("36");
        BigDecimal imc = new BigDecimal("32");

        BigDecimal ajustada = AntropometriaCalculator.ajustarCircPanturrilha(
                cpMedida, imc, PopulacaoReferencia.POPULACAO_CLINICA);
        assertThat(ajustada).isEqualByComparingTo("29");

        // Sem o ajuste, o eroERP diz "adequado" (36 > 34)
        assertThat(AntropometriaCalculator.classificarDeplecaoPanturrilha(
                cpMedida, Sexo.MASCULINO).tom()).isEqualTo(TomResultado.ADEQUADO);

        // Com o ajuste, é depleção muscular — a resposta oposta
        assertThat(AntropometriaCalculator.classificarDeplecaoPanturrilha(
                ajustada, Sexo.MASCULINO).tom()).isEqualTo(TomResultado.CRITICO);
    }

    @Test
    @DisplayName("fora da planilha — as duas colunas divergem só em IMC < 18,5")
    void asDuasColunasDePopulacao() {
        // Gonzalez 2021 soma no magro; a orientação GLIM não soma em doença
        // catabólica. Nas outras quatro faixas as colunas são idênticas.
        BigDecimal cp = new BigDecimal("30");

        assertThat(AntropometriaCalculator.ajustarCircPanturrilha(
                cp, new BigDecimal("17"), PopulacaoReferencia.ADULTO_SAUDAVEL))
                .isEqualByComparingTo("34");
        assertThat(AntropometriaCalculator.ajustarCircPanturrilha(
                cp, new BigDecimal("17"), PopulacaoReferencia.POPULACAO_CLINICA))
                .isEqualByComparingTo("30");

        for (String imc : new String[]{"20", "27", "35", "45"})
            assertThat(AntropometriaCalculator.ajustarCircPanturrilha(
                    cp, new BigDecimal(imc), PopulacaoReferencia.ADULTO_SAUDAVEL))
                    .as("IMC %s", imc)
                    .isEqualByComparingTo(AntropometriaCalculator.ajustarCircPanturrilha(
                            cp, new BigDecimal(imc), PopulacaoReferencia.POPULACAO_CLINICA));
    }

    @Test
    @DisplayName("fora da planilha — amputação sobreposta é recusada, não calculada errado")
    void amputacaoSobreposta() {
        // Membro superior (5,0 %) já inclui a mão (0,7 %). Marcar os dois
        // descontaria a mão duas vezes e devolveria peso menor que o real.
        var conflito = AntropometriaCalculator.conflitoDeSegmentos(
                EnumSet.of(SegmentoAmputado.MEMBRO_SUPERIOR, SegmentoAmputado.MAO));

        assertThat(conflito).isPresent();
        assertThat(conflito.get()).contains("Membro superior").contains("Mão");

        // Combinação coerente passa
        assertThat(AntropometriaCalculator.conflitoDeSegmentos(
                EnumSet.of(SegmentoAmputado.MAO, SegmentoAmputado.PE))).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal oleoPorAdministracao(BigDecimal colheres) {
        return artesanal(colheres).receitaPorAdministracao().stream()
                .filter(i -> i.produto().contains("Óleo"))
                .findFirst().orElseThrow().medidasPorAdministracao();
    }

    private DietaArtesanalCalculator.ResultadoArtesanal artesanal() {
        return artesanal(new BigDecimal("0.5"));
    }

    private DietaArtesanalCalculator.ResultadoArtesanal artesanal(BigDecimal colheresDeOleo) {
        return DietaArtesanalCalculator.calcular(
                new BigDecimal("2000"), PesoDeTrabalho.informado(new BigDecimal("75")),
                new DietaArtesanalCalculator.InsumoArtesanal("Trophic Basic",
                        new BigDecimal("7.8"), new BigDecimal("800"),
                        new BigDecimal("33.93"), new BigDecimal("4.68"),
                        new BigDecimal("1.24"), new BigDecimal("1.09")),
                null,
                new DietaArtesanalCalculator.InsumoArtesanal("Carbodex",
                        new BigDecimal("10"), new BigDecimal("500"),
                        new BigDecimal("40"), new BigDecimal("10"),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                new BigDecimal("3"),
                new DietaArtesanalCalculator.InsumoArtesanal("Albumix Power",
                        new BigDecimal("20"), new BigDecimal("500"),
                        new BigDecimal("70"), new BigDecimal("1.5"),
                        new BigDecimal("16"), BigDecimal.ZERO),
                new BigDecimal("2"),
                new DietaArtesanalCalculator.InsumoArtesanal("Óleo de soja",
                        new BigDecimal("13"), new BigDecimal("900"),
                        new BigDecimal("108"), BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("12")),
                colheresDeOleo, 4);
    }
}
