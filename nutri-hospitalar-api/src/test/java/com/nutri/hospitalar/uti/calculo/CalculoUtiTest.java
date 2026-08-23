package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.calculo.cascata.Altura;
import com.nutri.hospitalar.uti.calculo.cascata.MetaEnergetica;
import com.nutri.hospitalar.uti.calculo.cascata.MetaProteica;
import com.nutri.hospitalar.uti.calculo.cascata.PesoDeTrabalho;
import com.nutri.hospitalar.uti.calculo.cascata.VolumeDieta;
import com.nutri.hospitalar.uti.enums.EtniaChumlea;
import com.nutri.hospitalar.uti.enums.FaseTerapia;
import com.nutri.hospitalar.uti.enums.JanelaPerdaPeso;
import com.nutri.hospitalar.uti.enums.ModoInfusao;
import com.nutri.hospitalar.uti.enums.OrigemValor;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.enums.SegmentoAmputado;
import com.nutri.hospitalar.uti.enums.TerapiaRenal;
import com.nutri.hospitalar.uti.enums.TomResultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os gabaritos de {@code docs/10} §10 — todos conferidos contra os valores em
 * cache de {@code Facilita Nutri na UTI}, com a célula de origem no comentário.
 *
 * <p>Divergiu daqui, o código está errado — não o gabarito.
 */
@DisplayName("Cálculos da UTI adulto")
class CalculoUtiTest {

    // Entradas do caso canônico da aba Estimativas Antropométricas
    private static final BigDecimal CB = new BigDecimal("25");
    private static final BigDecimal AJ = new BigDecimal("53");
    private static final BigDecimal CA = new BigDecimal("90");
    private static final BigDecimal CP = new BigDecimal("34");
    private static final int IDADE = 59;

    @Nested
    @DisplayName("Antropometria")
    class Antropometria {

        @Test
        @DisplayName("altura estimada — Chumlea 1985")
        void alturaChumlea() {
            // Estimativas!B9 e B10
            conferir(AntropometriaCalculator.alturaChumlea1985(AJ, IDADE, Sexo.MASCULINO), "168.89");
            conferir(AntropometriaCalculator.alturaChumlea1985(AJ, IDADE, Sexo.FEMININO), "167.71");
        }

        @Test
        @DisplayName("peso estimado — Chumlea 1988, os oito ramos")
        void chumlea88OitoRamos() {
            // Estimativas!B13:B20, na ordem da planilha
            String[] esperados = {"54.75", "59.24", "52.55", "55.61",
                                  "57.74", "59.26", "57.49", "59.78"};
            int i = 0;

            for (Sexo sexo : List.of(Sexo.MASCULINO, Sexo.FEMININO))
                for (EtniaChumlea etnia : List.of(EtniaChumlea.BRANCA, EtniaChumlea.NEGRA))
                    for (int idade : new int[]{IDADE, 70})
                        conferir(EstimativaPesoCalculator.chumlea1988(AJ, CB, idade, sexo, etnia),
                                esperados[i++]);
        }

        @Test
        @DisplayName("peso estimado — Jung 2004")
        void jung() {
            // Estimativas!B24 e B25
            conferir(EstimativaPesoCalculator.jung2004(AJ, CB, IDADE, Sexo.MASCULINO), "60.845");
            conferir(EstimativaPesoCalculator.jung2004(AJ, CB, IDADE, Sexo.FEMININO), "57.345");
        }

        @Test
        @DisplayName("peso estimado — Rabito 2008")
        void rabito() {
            // Estimativas!B28 e B29. É o par que corrige o exemplo errado de docs/03 §12.
            conferir(EstimativaPesoCalculator.rabito2008(CB, CA, CP, Sexo.MASCULINO), "66.3083");
            conferir(EstimativaPesoCalculator.rabito2008(CB, CA, CP, Sexo.FEMININO), "61.4394");
        }

        @Test
        @DisplayName("IMC")
        void imc() {
            // Estimativas!B51
            conferir(AntropometriaCalculator.imc(new BigDecimal("62"), altura("175")), "20.2449");
        }

        @Test
        @DisplayName("peso ideal, ajustado e corrigido por amputação")
        void metasDePeso() {
            Altura altura = altura("168");

            // Estimativas!E4, E5, E6
            conferir(AntropometriaCalculator.pesoIdealPorImc(new BigDecimal("22"), altura), "62.0928");
            conferir(AntropometriaCalculator.pesoIdealPorImc(new BigDecimal("20.8"), altura), "58.70592");
            conferir(AntropometriaCalculator.pesoIdealPorImc(new BigDecimal("25"), altura), "70.56");

            // Estimativas!E9
            conferir(AntropometriaCalculator.pesoAjustado(
                    new BigDecimal("85"), new BigDecimal("58.71")), "67.3857");

            // Estimativas!G29 — antebraço = 1,6 %
            conferir(AntropometriaCalculator.pesoCorrigidoPorAmputacao(
                    new BigDecimal("65"), EnumSet.of(SegmentoAmputado.ANTEBRACO)), "63.96");
        }

        @Test
        @DisplayName("perda de peso — Blackburn 1977")
        void perdaDePeso() {
            // Estimativas!B32
            BigDecimal perda = AntropometriaCalculator.percentualPerdaPeso(
                    new BigDecimal("68"), new BigDecimal("60"));
            conferir(perda, "11.7647");

            // 11,76 % em um mês passa do corte de 5 % → grave
            Classificacao emUmMes = AntropometriaCalculator.classificarPerdaPeso(
                    perda, JanelaPerdaPeso.UM_MES);
            assertThat(emUmMes.tom()).isEqualTo(TomResultado.CRITICO);

            // O MESMO percentual em janelas diferentes continua grave até 6 meses,
            // porque 11,76 % passa de todos os cortes. O que muda é o rótulo.
            assertThat(AntropometriaCalculator.classificarPerdaPeso(
                    perda, JanelaPerdaPeso.SEIS_MESES).rotulo()).contains("6 meses");
        }

        @Test
        @DisplayName("a mesma perda muda de gravidade conforme a janela")
        void janelaMudaODiagnostico() {
            // 3 % é grave em uma semana e moderado em um mês. É por isso que a
            // janela é entrada obrigatória, e não a constante "1 mês" do eroERP.
            BigDecimal perda = new BigDecimal("3");

            assertThat(AntropometriaCalculator.classificarPerdaPeso(perda, JanelaPerdaPeso.UMA_SEMANA).tom())
                    .isEqualTo(TomResultado.CRITICO);
            assertThat(AntropometriaCalculator.classificarPerdaPeso(perda, JanelaPerdaPeso.UM_MES).tom())
                    .isEqualTo(TomResultado.ATENCAO);
        }

        @Test
        @DisplayName("adequação da circunferência do braço")
        void adequacaoCb() {
            // Estimativas!J25 — CB 30 contra P50 32,3
            BigDecimal adequacao = AntropometriaCalculator.adequacaoCircBraco(
                    new BigDecimal("30"), new BigDecimal("32.3"));
            conferir(adequacao, "92.8793");

            Classificacao classificacao =
                    AntropometriaCalculator.classificarAdequacaoCircBraco(adequacao);
            assertThat(classificacao.rotulo()).isEqualTo("Eutrofia");
            assertThat(classificacao.tom()).isEqualTo(TomResultado.ADEQUADO);
        }
    }

    @Nested
    @DisplayName("Necessidades nutricionais")
    class Necessidades {

        private final PesoDeTrabalho peso68 = PesoDeTrabalho.informado(new BigDecimal("68"));
        private final PesoDeTrabalho ideal82 = PesoDeTrabalho.informado(new BigDecimal("82"));

        @Test
        @DisplayName("faixa por fase da terapia")
        void porFase() {
            // Necessidades!B4/B5 e C4/C5
            var aguda = NecessidadeCalculator.energiaPorFase(FaseTerapia.AGUDA, peso68);
            conferir(aguda.minimo(), "1020");
            conferir(aguda.maximo(), "1360");

            var reabilitacao = NecessidadeCalculator.energiaPorFase(FaseTerapia.REABILITACAO, peso68);
            conferir(reabilitacao.minimo(), "1700");
            conferir(reabilitacao.maximo(), "2040");

            var ptnAguda = NecessidadeCalculator.proteinaPorFase(FaseTerapia.AGUDA, peso68);
            conferir(ptnAguda.minimo(), "81.6");
            conferir(ptnAguda.maximo(), "102");

            var ptnReab = NecessidadeCalculator.proteinaPorFase(FaseTerapia.REABILITACAO, peso68);
            conferir(ptnReab.minimo(), "102");
            conferir(ptnReab.maximo(), "136");
        }

        @Test
        @DisplayName("terapia renal substitutiva")
        void terapiaRenal() {
            // Necessidades!A9:B10
            conferir(NecessidadeCalculator.proteinaTerapiaRenal(
                    TerapiaRenal.HEMODIALISE_INTERMITENTE, peso68), "122.4");
            conferir(NecessidadeCalculator.proteinaTerapiaRenal(
                    TerapiaRenal.HEMODIALISE_CONTINUA, peso68), "136");

            // Ausência é ausência, não zero
            assertThat(NecessidadeCalculator.proteinaTerapiaRenal(TerapiaRenal.NENHUMA, peso68))
                    .isNull();
        }

        @Test
        @DisplayName("obesidade — a base do peso muda com o IMC")
        void obesidade() {
            // Necessidades!B15/B16 usam peso ATUAL; C15/C16 e a proteína usam IDEAL
            var ate40 = NecessidadeCalculator.energiaObesidade(
                    new BigDecimal("32"), peso68, ideal82);
            conferir(ate40.minimo(), "748");
            conferir(ate40.maximo(), "952");

            var acima40 = NecessidadeCalculator.energiaObesidade(
                    new BigDecimal("42"), peso68, ideal82);
            conferir(acima40.minimo(), "1804");
            conferir(acima40.maximo(), "2050");

            conferir(NecessidadeCalculator.proteinaObesidade(new BigDecimal("32"), ideal82), "164");
            conferir(NecessidadeCalculator.proteinaObesidade(new BigDecimal("52"), ideal82), "205");
        }

        @Test
        @DisplayName("personalizado")
        void personalizado() {
            // Necessidades!F5:G9
            conferir(NecessidadeCalculator.energiaPersonalizada(new BigDecimal("35"), peso68).kcalDia(),
                    "2380");
            conferir(NecessidadeCalculator.proteinaPersonalizada(new BigDecimal("1.3"), peso68).gramasDia(),
                    "88.4");
        }
    }

    @Nested
    @DisplayName("Dieta enteral")
    class DietaEnteral {

        @Test
        @DisplayName("contínua — 68 kg, Peptamen Intense, 62 ml/h por 22 h")
        void contínua() {
            PesoDeTrabalho peso = PesoDeTrabalho.informado(new BigDecimal("68"));
            MetaEnergetica meta = new MetaEnergetica(new BigDecimal("1360"), OrigemValor.META_POR_FAIXA);
            MetaProteica metaPtn = new MetaProteica(new BigDecimal("102"), OrigemValor.META_POR_FAIXA);
            BigDecimal densidade = new BigDecimal("1.0");
            BigDecimal ptnPorLitro = new BigDecimal("92");
            BigDecimal tempo = new BigDecimal("22");

            VolumeDieta vt = DietaEnteralCalculator.volumeTotal(
                    new BigDecimal("62"), tempo, ModoInfusao.CONTINUA);
            conferir(vt.ml(), "1364");

            BigDecimal kcal = DietaEnteralCalculator.caloriasOfertadas(densidade, vt);
            BigDecimal ptn = DietaEnteralCalculator.proteinaOfertada(ptnPorLitro, vt);
            conferir(kcal, "1364");
            conferir(ptn, "125.488");

            conferir(DietaEnteralCalculator.caloriasPorQuilo(kcal, peso), "20.0588");
            conferir(DietaEnteralCalculator.proteinaPorQuilo(ptn, peso), "1.8454");
            conferir(DietaEnteralCalculator.percentualDoVct(kcal, meta), "100.2941");
            conferir(DietaEnteralCalculator.percentualDaProteina(ptn, metaPtn), "123.0275");

            BigDecimal pleno = DietaEnteralCalculator.volumePleno(meta, densidade, tempo);
            conferir(pleno, "61.8182");
            conferir(DietaEnteralCalculator.proteinaNoVolumePleno(pleno, tempo, ptnPorLitro), "125.12");
        }

        @Test
        @DisplayName("intermitente — 65 kg, Novasource Senior, 133 ml em 6 horários")
        void intermitente() {
            MetaEnergetica meta = new MetaEnergetica(new BigDecimal("1800"), OrigemValor.META_POR_FAIXA);
            BigDecimal densidade = new BigDecimal("1.24");
            BigDecimal ptnPorLitro = new BigDecimal("65");
            BigDecimal tempo = new BigDecimal("6");

            VolumeDieta vt = DietaEnteralCalculator.volumeTotal(
                    new BigDecimal("133"), tempo, ModoInfusao.INTERMITENTE);
            conferir(vt.ml(), "798");
            conferir(DietaEnteralCalculator.proteinaOfertada(ptnPorLitro, vt), "51.87");

            BigDecimal pleno = DietaEnteralCalculator.volumePleno(meta, densidade, tempo);
            conferir(pleno, "241.9355");
            conferir(DietaEnteralCalculator.proteinaNoVolumePleno(pleno, tempo, ptnPorLitro), "94.3548");
        }

        @Test
        @DisplayName("o modo não muda a matemática — mesmo VT, mesmo resultado")
        void modoNaoMudaAMatematica() {
            // A planilha tem duas abas quase idênticas e cinco defeitos nasceram
            // da divergência entre elas. Aqui é uma classe só, e este teste é o
            // que fixa isso.
            BigDecimal densidade = new BigDecimal("1.5");
            BigDecimal ptnPorLitro = new BigDecimal("75");

            VigenciaDieta continua = calcular(new BigDecimal("50"), new BigDecimal("20"),
                    ModoInfusao.CONTINUA, densidade, ptnPorLitro);
            VigenciaDieta intermitente = calcular(new BigDecimal("125"), new BigDecimal("8"),
                    ModoInfusao.INTERMITENTE, densidade, ptnPorLitro);

            assertThat(continua.vt).isEqualByComparingTo(intermitente.vt);
            assertThat(continua.kcal).isEqualByComparingTo(intermitente.kcal);
            assertThat(continua.ptn).isEqualByComparingTo(intermitente.ptn);
        }

        @Test
        @DisplayName("progressão dos dias 1 a 4")
        void progressao() {
            // Contínuo!P15:V18
            MetaEnergetica meta = new MetaEnergetica(new BigDecimal("1360"), OrigemValor.META_POR_FAIXA);
            var degraus = DietaEnteralCalculator.progressao(
                    meta, new BigDecimal("1.5"), new BigDecimal("22"));

            assertThat(degraus).hasSize(4);
            conferir(degraus.get(0).volume(), "10.3030");
            conferir(degraus.get(1).volume(), "20.6061");
            conferir(degraus.get(2).volume(), "30.9091");
            conferir(degraus.get(3).volume(), "41.2121");

            // E em horários, o mesmo cálculo dá números redondos
            var emHorarios = DietaEnteralCalculator.progressao(
                    new MetaEnergetica(new BigDecimal("1800"), OrigemValor.META_POR_FAIXA),
                    new BigDecimal("1.5"), new BigDecimal("6"));
            conferir(emHorarios.get(0).volume(), "50");
            conferir(emHorarios.get(3).volume(), "200");
        }

        @Test
        @DisplayName("módulo proteico — Nutren Just Protein para uma lacuna de 45 g")
        void moduloProteico() {
            // Contínuo!Q21:V24 — medida 15 g, 13 g PTN, 52 kcal
            var sugestao = DietaEnteralCalculator.moduloProteico(
                    new BigDecimal("45"), new BigDecimal("15"),
                    new BigDecimal("13"), new BigDecimal("52"));

            conferir(sugestao.gramas(), "51.9231");
            conferir(sugestao.kcal(), "180.0");
        }

        private VigenciaDieta calcular(BigDecimal volume, BigDecimal tempo, ModoInfusao modo,
                                       BigDecimal densidade, BigDecimal ptnPorLitro) {
            VolumeDieta vt = DietaEnteralCalculator.volumeTotal(volume, tempo, modo);
            return new VigenciaDieta(vt.ml(),
                    DietaEnteralCalculator.caloriasOfertadas(densidade, vt),
                    DietaEnteralCalculator.proteinaOfertada(ptnPorLitro, vt));
        }

        private record VigenciaDieta(BigDecimal vt, BigDecimal kcal, BigDecimal ptn) {}
    }

    @Nested
    @DisplayName("Hidratação")
    class Hidratacao {

        private final PesoDeTrabalho peso72 = PesoDeTrabalho.informado(new BigDecimal("72"));

        @Test
        @DisplayName("necessidade de 25 a 30 ml/kg")
        void necessidade() {
            conferir(HidratacaoCalculator.necessidadeMinima(peso72), "1800");
            conferir(HidratacaoCalculator.necessidadeIdeal(peso72), "2160");
        }

        @Test
        @DisplayName("água na dieta e água extra, nas quatro densidades da tabela")
        void aguaEExtra() {
            // Hidratação!D17:I21, dieta de 1600 ml
            VolumeDieta dieta = new VolumeDieta(new BigDecimal("1600"), "1600 ml");
            BigDecimal ideal = HidratacaoCalculator.necessidadeIdeal(peso72);

            String[][] casos = {{"2.0", "1120", "1040"}, {"1.5", "1200", "960"},
                                {"1.2", "1280", "880"}, {"1.0", "1360", "800"}};

            for (String[] caso : casos) {
                var perc = HidratacaoCalculator.percentualDeAgua(null, new BigDecimal(caso[0]));
                BigDecimal naDieta = HidratacaoCalculator.aguaNaDieta(dieta, perc);
                conferir(naDieta, caso[1]);
                conferir(HidratacaoCalculator.aguaExtra(ideal, naDieta), caso[2]);
            }
        }

        @Test
        @DisplayName("o % de água diz de onde veio")
        void procedenciaDaAgua() {
            // Do rótulo, quando o produto declara
            var doRotulo = HidratacaoCalculator.percentualDeAgua(
                    new BigDecimal("76"), new BigDecimal("1.5"));
            assertThat(doRotulo.origem()).isEqualTo(OrigemValor.AGUA_LIVRE_ROTULO);
            conferir(doRotulo.valor(), "76");

            // Da escada por densidade, quando não declara — e dizendo que estimou
            var estimada = HidratacaoCalculator.percentualDeAgua(null, new BigDecimal("1.5"));
            assertThat(estimada.origem()).isEqualTo(OrigemValor.AGUA_LIVRE_ESTIMADA);
            conferir(estimada.valor(), "75");

            // Densidade intermediária não tem linha: ausência, não degrau inventado
            assertThat(HidratacaoCalculator.percentualDeAgua(null, new BigDecimal("1.24"))).isNull();
        }

        @Test
        @DisplayName("distribuição da água extra")
        void distribuicao() {
            // Hidratação!F16:I16 — extra ideal de 1040 ml
            var fracoes = HidratacaoCalculator.distribuir(new BigDecimal("1040"));
            assertThat(fracoes).hasSize(4);
            conferir(fracoes.get(0).mlPorVez(), "260");      // 4×
            conferir(fracoes.get(2).mlPorVez(), "173.3333"); // 6×
        }
    }

    @Nested
    @DisplayName("Ferramentas clínicas")
    class Ferramentas {

        private final PesoDeTrabalho peso70 = PesoDeTrabalho.informado(new BigDecimal("70"));

        @Test
        @DisplayName("noradrenalina — as duas assinaturas concordam")
        void noradrenalina() {
            // Cálculos!C3, C4 e J6. Os presets "32 simples" e "64 concentrada"
            // são 2 e 4 ampolas em 250 ml — e é isso que este teste prova.
            BigDecimal simples = FerramentaClinicaCalculator.concentracaoNoradrenalina(
                    new BigDecimal("2"), new BigDecimal("250"));
            BigDecimal concentrada = FerramentaClinicaCalculator.concentracaoNoradrenalina(
                    new BigDecimal("4"), new BigDecimal("250"));

            conferir(simples, "32");
            conferir(concentrada, "64");

            conferir(FerramentaClinicaCalculator.doseNoradrenalina(
                    new BigDecimal("25"), simples, peso70), "0.190476");
            conferir(FerramentaClinicaCalculator.doseNoradrenalina(
                    new BigDecimal("25"), concentrada, peso70), "0.380952");

            // O exemplo de J5: 4 ampolas em 234 ml, a 20 ml/h
            BigDecimal daBolsa = FerramentaClinicaCalculator.concentracaoNoradrenalina(
                    new BigDecimal("4"), new BigDecimal("234"));
            conferir(FerramentaClinicaCalculator.doseNoradrenalina(
                    new BigDecimal("20"), daBolsa, peso70), "0.325600");
        }

        @Test
        @DisplayName("balanço nitrogenado")
        void balancoNitrogenado() {
            // Cálculos!C11:B13
            var balanco = FerramentaClinicaCalculator.balancoNitrogenado(
                    new BigDecimal("95"), new BigDecimal("40"));

            conferir(balanco.nitrogenioIngerido(), "15.2");
            conferir(balanco.nitrogenioExcretado(), "22.6916");
            conferir(balanco.balanco(), "-7.4916");

            // Negativo é catabolismo, e o resultado diz isso por escrito
            assertThat(balanco.classificacao().rotulo()).contains("catabolismo");
            assertThat(balanco.classificacao().tom()).isEqualTo(TomResultado.ATENCAO);
        }

        @Test
        @DisplayName("calorias do propofol")
        void propofol() {
            // Cálculos!B18
            conferir(FerramentaClinicaCalculator.caloriasPropofol(
                    new BigDecimal("20"), new BigDecimal("24")), "528");

            // As 24 h são constante escondida na planilha; aqui são o padrão
            conferir(FerramentaClinicaCalculator.caloriasPropofol(
                    new BigDecimal("20"), null), "528");

            // E metade do dia entrega metade da caloria
            conferir(FerramentaClinicaCalculator.caloriasPropofol(
                    new BigDecimal("20"), new BigDecimal("12")), "264");
        }
    }

    @Nested
    @DisplayName("Dieta artesanal — sistema aberto")
    class Artesanal {

        @Test
        @DisplayName("o caso canônico, e o fechamento por Atwater")
        void casoCanonico() {
            // TNE Sistema aberto — VET 2000, peso 75, óleo 0,5 · Carbodex 3 · Albumix 2
            var resultado = DietaArtesanalCalculator.calcular(
                    new BigDecimal("2000"), PesoDeTrabalho.informado(new BigDecimal("75")),
                    trophic(), null,
                    carbodex(), new BigDecimal("3"),
                    albumix(), new BigDecimal("2"),
                    oleo(), new BigDecimal("0.5"),
                    4);

            conferir(resultado.dosesBase(), "48.7504");

            conferir(resultado.choDaBase(), "228.1517");
            conferir(resultado.ptnDaBase(), "60.4505");
            conferir(resultado.lipDaBase(), "53.1379");

            conferir(resultado.choTotal(), "261.1517");
            conferir(resultado.ptnTotal(), "92.4505");
            conferir(resultado.lipTotal(), "59.1379");

            conferir(resultado.kcalBase(), "1826.6498");
            conferir(resultado.kcalTotal(), "1946.6498");

            conferir(resultado.kcalPorQuilo(), "25.9553");
            conferir(resultado.proteinaPorQuilo(), "1.2327");

            conferir(resultado.aguaTotal(), "1789.3855");
            conferir(resultado.aguaPorAdministracao(), "447.3464");
        }

        @Test
        @DisplayName("os dois caminhos para a kcal total dão o mesmo número")
        void kcalDeMacrosFecha() {
            // É a validação que a própria planilha oferece: a soma dos macros por
            // Atwater bate com a kcal composta produto a produto. Confirma
            // UtiMatematica.kcalDeMacros contra a fonte.
            var resultado = DietaArtesanalCalculator.calcular(
                    new BigDecimal("2000"), PesoDeTrabalho.informado(new BigDecimal("75")),
                    trophic(), null,
                    carbodex(), new BigDecimal("3"),
                    albumix(), new BigDecimal("2"),
                    oleo(), new BigDecimal("0.5"),
                    4);

            assertThat(resultado.kcalPorMacros().setScale(4, RoundingMode.HALF_UP))
                    .isEqualByComparingTo(resultado.kcalTotal().setScale(4, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("os dois denominadores de percentual, nomeados")
        void doisDenominadores() {
            var resultado = DietaArtesanalCalculator.calcular(
                    new BigDecimal("2000"), PesoDeTrabalho.informado(new BigDecimal("75")),
                    trophic(), null,
                    carbodex(), new BigDecimal("3"),
                    albumix(), new BigDecimal("2"),
                    oleo(), new BigDecimal("0.5"),
                    4);

            // Sobre o VET desejado — os números da planilha, que somam 97,33 %
            conferir(resultado.percChoSobreVet(), "52.2303");
            conferir(resultado.percPtnSobreVet(), "18.4901");
            conferir(resultado.percLipSobreVet(), "26.6121");

            // Sobre o ofertado — soma 100 por construção
            BigDecimal soma = resultado.percChoSobreOfertado()
                    .add(resultado.percPtnSobreOfertado())
                    .add(resultado.percLipSobreOfertado());
            conferir(soma, "100");
        }

        private DietaArtesanalCalculator.InsumoArtesanal trophic() {
            return new DietaArtesanalCalculator.InsumoArtesanal("Trophic Basic",
                    new BigDecimal("7.8"), new BigDecimal("800"),
                    new BigDecimal("33.93"), new BigDecimal("4.68"),
                    new BigDecimal("1.24"), new BigDecimal("1.09"));
        }

        private DietaArtesanalCalculator.InsumoArtesanal carbodex() {
            return new DietaArtesanalCalculator.InsumoArtesanal("Carbodex",
                    new BigDecimal("10"), new BigDecimal("500"),
                    new BigDecimal("40"), new BigDecimal("10"),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        private DietaArtesanalCalculator.InsumoArtesanal albumix() {
            return new DietaArtesanalCalculator.InsumoArtesanal("Albumix Power",
                    new BigDecimal("20"), new BigDecimal("500"),
                    new BigDecimal("70"), new BigDecimal("1.5"),
                    new BigDecimal("16"), BigDecimal.ZERO);
        }

        private DietaArtesanalCalculator.InsumoArtesanal oleo() {
            return new DietaArtesanalCalculator.InsumoArtesanal("Óleo de soja",
                    new BigDecimal("13"), new BigDecimal("900"),
                    new BigDecimal("108"), BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("12"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Confere na escala do próprio gabarito: 168,89 compara com duas casas,
     * 66,3083 com quatro. Evita ter de escolher uma escala única que sirva a
     * valores exatos e a dízimas ao mesmo tempo.
     */
    private static void conferir(BigDecimal calculado, String esperado) {
        BigDecimal alvo = new BigDecimal(esperado);
        assertThat(calculado)
                .as("esperado %s", esperado)
                .isNotNull();
        assertThat(calculado.setScale(alvo.scale(), RoundingMode.HALF_UP))
                .as("esperado %s", esperado)
                .isEqualByComparingTo(alvo);
    }

    private static Altura altura(String centimetros) {
        return Altura.informadaEmCentimetros(new BigDecimal(centimetros));
    }

    @SuppressWarnings("unused")
    private static final PopulacaoReferencia PADRAO = PopulacaoReferencia.POPULACAO_CLINICA;
}
