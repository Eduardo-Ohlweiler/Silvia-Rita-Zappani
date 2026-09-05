package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;

import java.math.BigDecimal;

import static com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator.cem;
import static com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator.conta;
import static com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator.escala;
import static com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator.faixa;
import static com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator.percentual;
import static com.nutri.hospitalar.pediatria.calculo.CalculoPediatricoCalculator.positivo;

/**
 * O que se deriva de um dia de acompanhamento pediátrico. Especificação em
 * {@code docs/11} §5.
 *
 * <p><b>Vive neste pacote de propósito.</b> A régua da OMS —
 * {@link CalculoPediatricoCalculator#faixa} — é package-private, e ela é a mesma
 * aqui e na avaliação: uma segunda cópia divergiria, e divergência em
 * classificação de crescimento infantil é diagnóstico errado. Pela mesma razão a
 * escala e o arredondamento são os de lá, não uma repetição.
 *
 * <p><b>A armadilha que esta classe existe para não cair:</b> a UTI adulto tem
 * {@code AcompanhamentoCalculator} com as mesmas quatro contas, e ele divide a
 * composição por <b>1000</b>, porque o catálogo enteral é declarado por litro. A
 * fórmula láctea é declarada <b>por 100 ml</b> ({@code docs/09} §7). Reusar a
 * função da UTI daria <b>dez vezes</b> o valor, num número que vira adequação
 * calórica na tela de quem prescreve.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco. A linha da OMS chega
 * resolvida, como em {@link CalculoPediatricoCalculator}.
 */
public final class AcompanhamentoPediatricoCalculator {

    private AcompanhamentoPediatricoCalculator() {}

    // Motivos de ausência — o texto que a tela mostra no lugar do traço.
    static final String SEM_NASCIMENTO =
            "Cadastre a data de nascimento do paciente: a idade do dia sai dela";
    static final String SEM_SEXO =
            "Informe o sexo do paciente: as curvas da OMS são separadas por sexo";
    static final String FORA_DA_CURVA =
            "As curvas de crescimento da OMS vão até 60 meses";
    static final String SEM_MEDIDAS =
            "Informe o peso ou a estatura do dia para classificar";
    static final String SEM_ESTATURA_PARA_IMC =
            "Informe a estatura para calcular o IMC do dia";
    static final String SEM_PESO_PARA_IMC =
            "Informe o peso para calcular o IMC do dia";
    static final String SEM_RECEBIDO =
            "Informe o volume recebido no dia";
    static final String SEM_PRESCRITO =
            "Sem volume prescrito não há contra o quê comparar: informe o prescrito "
                    + "do dia, ou vincule a avaliação";
    static final String SEM_FORMULA =
            "Vincule uma avaliação com fórmula láctea para saber o que o volume entrega";
    static final String SEM_PESO_DO_DIA =
            "Informe o peso do dia para ver kcal/kg e g/kg";
    static final String SEM_VET =
            "Vincule uma avaliação com VET para medir a adequação calórica";
    static final String SEM_PROTEINA_META =
            "Vincule uma avaliação com necessidade proteica para medir a adequação";
    static final String SEM_TOMADAS =
            "Informe as tomadas previstas e as aceitas";

    /** Referências do percentual recebido — a tela diz contra o quê comparou. */
    static final String PRESCRITO_DA_AVALIACAO = "do volume prescrito na avaliação";
    static final String PRESCRITO_DO_DIA = "do volume prescrito informado no dia";

    /**
     * @param entrada as entradas do dia, já normalizadas
     * @param linha   a linha da OMS para o sexo e a idade <b>do dia</b>, ou
     *                {@code null} quando não existe — idade fora de 0 a 60
     *                meses, sexo ausente, ou nascimento não cadastrado
     */
    public static ResultadoAcompanhamento calcular(EntradaAcompanhamento entrada,
                                                   LinhaPercentil linha) {
        BigDecimal peso = entrada.pesoKg();
        BigDecimal estatura = entrada.estaturaCm();
        Integer idade = entrada.idadeMeses();

        // ─── Idade do dia ────────────────────────────────────────────────
        String motivoIdade = idade == null ? SEM_NASCIMENTO : null;

        // ─── IMC do dia ──────────────────────────────────────────────────
        BigDecimal imc = null;
        String motivoImc = null;
        if (positivo(peso) && positivo(estatura)) {
            BigDecimal metros = estatura.divide(cem(), conta());
            imc = peso.divide(metros.multiply(metros, conta()), conta());
        } else if (!positivo(estatura)) {
            motivoImc = SEM_ESTATURA_PARA_IMC;
        } else {
            motivoImc = SEM_PESO_PARA_IMC;
        }

        // ─── Estado nutricional, com as medidas DO DIA ───────────────────
        //
        // Classifica de novo, e não repete a classificação da avaliação: é o
        // ponto todo do acompanhamento. Uma criança que era P85 exato na
        // avaliação e ganhou 200 g passa para "acima do peso", e é isso que a
        // série tem de mostrar.
        FaixaOms pesoIdade = null;
        FaixaOms estaturaIdade = null;
        FaixaOms imcIdade = null;
        String motivoEstado = null;

        if (entrada.sexo() == null) {
            motivoEstado = SEM_SEXO;
        } else if (idade == null) {
            motivoEstado = SEM_NASCIMENTO;
        } else if (linha == null) {
            motivoEstado = FORA_DA_CURVA;
        } else {
            if (positivo(peso))     pesoIdade     = faixa(peso,     linha.pesoP15(),     linha.pesoP85());
            if (positivo(estatura)) estaturaIdade = faixa(estatura, linha.estaturaP15(), linha.estaturaP85());
            if (imc != null)        imcIdade      = faixa(imc,      linha.imcP15(),      linha.imcP85());

            if (pesoIdade == null && estaturaIdade == null && imcIdade == null)
                motivoEstado = SEM_MEDIDAS;
        }

        // ─── Percentual recebido ─────────────────────────────────────────
        //
        // O prescrito da AVALIAÇÃO vence o digitado no dia: é o que estava de
        // fato prescrito, e não depende de alguém repetir o número certo. Mesma
        // regra de AcompanhamentoCalculator.percentualRecebido, na UTI.
        BigDecimal recebido = entrada.volRecebido24h();
        boolean daAvaliacao = positivo(entrada.volumePrescritoNaAvaliacao());
        BigDecimal prescrito = daAvaliacao
                ? entrada.volumePrescritoNaAvaliacao() : entrada.volPrescrito24h();

        /*
         * A procedência acompanha o PRESCRITO, não o percentual: um dia já
         * prescrito e ainda sem recebido diz contra o que a adesão vai ser
         * medida, em vez de mostrar o volume sem nome.
         */
        String referenciaRecebido = positivo(prescrito)
                ? (daAvaliacao ? PRESCRITO_DA_AVALIACAO : PRESCRITO_DO_DIA) : null;

        BigDecimal percRecebido = null;
        String motivoPercRecebido;
        if (recebido == null) {
            motivoPercRecebido = SEM_RECEBIDO;
        } else if (!positivo(prescrito)) {
            motivoPercRecebido = SEM_PRESCRITO;
        } else {
            percRecebido = recebido.multiply(cem(), conta()).divide(prescrito, conta());
            motivoPercRecebido = null;
        }

        // ─── O que o volume recebido entrega ─────────────────────────────
        //
        // /100 e NÃO /1000: a fórmula láctea é declarada por 100 ml.
        BigDecimal kcal100 = entrada.formulaKcalPor100ml();
        BigDecimal ptn100 = entrada.formulaProteinaPor100ml();

        BigDecimal calorias = null;
        BigDecimal proteina = null;
        String motivoOferta;
        if (recebido == null) {
            motivoOferta = SEM_RECEBIDO;
        } else if (kcal100 == null && ptn100 == null) {
            motivoOferta = SEM_FORMULA;
        } else {
            if (kcal100 != null) calorias = kcal100.multiply(recebido, conta()).divide(cem(), conta());
            if (ptn100 != null)  proteina = ptn100.multiply(recebido, conta()).divide(cem(), conta());
            motivoOferta = null;
        }

        // ─── Por quilo de peso do dia ────────────────────────────────────
        BigDecimal kcalPorKg = null;
        BigDecimal ptnPorKg = null;
        String motivoPorQuilo;
        if (calorias == null && proteina == null) {
            motivoPorQuilo = motivoOferta;
        } else if (!positivo(peso)) {
            motivoPorQuilo = SEM_PESO_DO_DIA;
        } else {
            if (calorias != null) kcalPorKg = calorias.divide(peso, conta());
            if (proteina != null) ptnPorKg = proteina.divide(peso, conta());
            motivoPorQuilo = null;
        }

        // ─── Adequações contra as metas da avaliação ─────────────────────
        BigDecimal vet = entrada.vetDaAvaliacao();
        BigDecimal adequacaoCalorica = null;
        String motivoAdeqCalorica;
        if (calorias == null) {
            motivoAdeqCalorica = motivoOferta;
        } else if (!positivo(vet)) {
            motivoAdeqCalorica = SEM_VET;
        } else {
            adequacaoCalorica = calorias.multiply(cem(), conta()).divide(vet, conta());
            motivoAdeqCalorica = null;
        }

        BigDecimal metaProteica = entrada.proteinaNecessidadeDaAvaliacao();
        BigDecimal adequacaoProteica = null;
        String motivoAdeqProteica;
        if (proteina == null) {
            motivoAdeqProteica = motivoOferta;
        } else if (!positivo(metaProteica)) {
            motivoAdeqProteica = SEM_PROTEINA_META;
        } else {
            adequacaoProteica = proteina.multiply(cem(), conta()).divide(metaProteica, conta());
            motivoAdeqProteica = null;
        }

        // ─── Aceitação das tomadas ───────────────────────────────────────
        Integer previstas = entrada.tomadasPrevistas();
        Integer aceitas = entrada.tomadasAceitas();

        BigDecimal aceitacao = null;
        String motivoAceitacao;
        if (previstas == null || aceitas == null || previstas <= 0) {
            motivoAceitacao = SEM_TOMADAS;
        } else {
            aceitacao = new BigDecimal(aceitas)
                    .multiply(cem(), conta())
                    .divide(new BigDecimal(previstas), conta());
            motivoAceitacao = null;
        }

        return new ResultadoAcompanhamento(
                idade, motivoIdade,
                escala(imc), motivoImc,
                pesoIdade, estaturaIdade, imcIdade, motivoEstado,

                escala(positivo(prescrito) ? prescrito : null),
                percentual(percRecebido), referenciaRecebido, motivoPercRecebido,
                escala(calorias), escala(proteina), motivoOferta,
                escala(kcalPorKg), escala(ptnPorKg), motivoPorQuilo,
                percentual(adequacaoCalorica), motivoAdeqCalorica,
                percentual(adequacaoProteica), motivoAdeqProteica,
                percentual(aceitacao), motivoAceitacao);
    }
}
