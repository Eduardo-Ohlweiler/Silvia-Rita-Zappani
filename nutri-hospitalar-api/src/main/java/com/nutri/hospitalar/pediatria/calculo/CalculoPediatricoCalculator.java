package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Os cálculos da avaliação pediátrica: estado nutricional pela OMS,
 * necessidades pelas DRIs 2002 e adequação da dieta láctea prescrita.
 *
 * <p><b>A especificação é {@code docs/09-calculos-pediatria.md}</b>, extraída
 * célula a célula da planilha {@code Pediatria.xlsx}. Divergiu da planilha,
 * este código está errado — não o contrário.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco. Recebe as entradas já
 * resolvidas (inclusive a linha de percentil, que quem busca é o service) e
 * devolve números. É trivial de testar e não consegue acessar banco por
 * acidente.
 *
 * <p>Contas intermediárias correm em {@link MathContext#DECIMAL64} — 16 dígitos
 * significativos, a mesma ordem de precisão do LibreOffice — e o arredondamento
 * acontece UMA vez, na saída. Arredondar a cada passo faria o volume total
 * divergir da planilha em frequências que não dividem 24.
 */
public final class CalculoPediatricoCalculator {

    private CalculoPediatricoCalculator() {}

    /** Precisão das contas intermediárias, antes do arredondamento final. */
    private static final MathContext CONTA = MathContext.DECIMAL64;

    /** Escala dos resultados — casa com NUMERIC(12,4) das colunas. */
    private static final int ESCALA = 4;

    /** Escala dos percentuais — casa com NUMERIC(6,2). */
    private static final int ESCALA_PERCENTUAL = 2;

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal VINTE_E_QUATRO = new BigDecimal("24");

    // Motivos de ausência — texto que a tela mostra no lugar do traço.
    private static final String SEM_ESTATURA =
            "Informe a estatura para calcular o IMC";
    private static final String SEM_SEXO =
            "Informe o sexo: as curvas da OMS são separadas por sexo";
    private static final String SEM_IDADE =
            "Informe a idade em meses";
    private static final String FORA_DA_CURVA =
            "As curvas de crescimento da OMS vão até 60 meses";
    private static final String VET_FORA_DA_FAIXA =
            "O VET das DRIs 2002 é definido até 35 meses";
    private static final String PROTEINA_FORA_DA_FAIXA =
            "A necessidade proteica das DRIs 2002 é definida até 36 meses";
    private static final String SEM_FREQUENCIA =
            "Informe a frequência (intervalo em horas) para calcular a dieta";
    private static final String SEM_VOLUME =
            "Informe o volume por tomada";
    private static final String SEM_FORMULA =
            "Escolha a fórmula láctea para calcular calorias e proteína ofertadas";
    private static final String SEM_PESO =
            "Informe o peso";
    private static final String SEM_PESO_PARA_IMC =
            "Informe o peso para calcular o IMC";
    private static final String SEM_MEDIDAS =
            "Informe o peso ou a estatura para classificar o estado nutricional";

    /**
     * @param entrada as entradas já normalizadas
     * @param linha   a linha da tabela da OMS para o sexo e a idade da criança,
     *                ou {@code null} quando não existe — idade fora de 0 a 60
     *                meses, ou sexo não informado
     */
    public static ResultadoPediatrico calcular(EntradaPediatrica entrada, LinhaPercentil linha) {
        Integer idade = entrada.idadeMeses();
        BigDecimal peso = entrada.peso();
        BigDecimal estatura = entrada.estatura();

        // ─── IMC ─────────────────────────────────────────────────────────
        BigDecimal imc = null;
        String motivoImc = null;
        if (positivo(peso) && positivo(estatura)) {
            BigDecimal metros = estatura.divide(CEM, CONTA);
            imc = peso.divide(metros.multiply(metros, CONTA), CONTA);
        } else if (!positivo(estatura)) {
            motivoImc = SEM_ESTATURA;
        } else {
            // Estatura veio, peso não: sem este ramo o IMC saía nulo E mudo.
            motivoImc = SEM_PESO_PARA_IMC;
        }

        // ─── Estado nutricional pela OMS ─────────────────────────────────
        FaixaOms pesoIdade = null;
        FaixaOms estaturaIdade = null;
        FaixaOms imcIdade = null;
        String motivoEstado = null;

        if (entrada.sexo() == null) {
            motivoEstado = SEM_SEXO;
        } else if (idade == null) {
            motivoEstado = SEM_IDADE;
        } else if (linha == null) {
            motivoEstado = FORA_DA_CURVA;
        } else {
            if (positivo(peso))     pesoIdade     = faixa(peso,     linha.pesoP15(),     linha.pesoP85());
            if (positivo(estatura)) estaturaIdade = faixa(estatura, linha.estaturaP15(), linha.estaturaP85());
            if (imc != null)        imcIdade      = faixa(imc,      linha.imcP15(),      linha.imcP85());

            // A linha da OMS existe, mas não há o que comparar com ela: sem
            // este ramo as três classificações saíam nulas e sem uma palavra.
            if (pesoIdade == null && estaturaIdade == null && imcIdade == null)
                motivoEstado = SEM_MEDIDAS;
        }

        // ─── VET e necessidade proteica ──────────────────────────────────
        BigDecimal vet = null;
        String motivoVet = null;
        BigDecimal deposicao = deposicaoEnergetica(idade);
        if (idade == null) {
            motivoVet = SEM_IDADE;
        } else if (deposicao == null) {
            motivoVet = VET_FORA_DA_FAIXA;
        } else if (positivo(peso)) {
            vet = vet(peso, deposicao);
        } else {
            // A idade está na faixa e a deposição existe; falta o peso, que é
            // o que multiplica. Sem este ramo o VET saía nulo e mudo — e o
            // peso é opcional de propósito, para quem preenche aos poucos.
            motivoVet = SEM_PESO;
        }

        BigDecimal proteinaNecessidade = necessidadeProteica(idade);
        String motivoProteina = null;
        if (idade == null) {
            motivoProteina = SEM_IDADE;
        } else if (proteinaNecessidade == null) {
            motivoProteina = PROTEINA_FORA_DA_FAIXA;
        }

        // ─── Dieta láctea prescrita ──────────────────────────────────────
        BigDecimal vezesDia = null;
        BigDecimal volumeTotal = null;
        BigDecimal caloriasTotais = null;
        BigDecimal proteinaTotal = null;
        BigDecimal percCalorico = null;
        BigDecimal percProteico = null;
        String motivoDieta = null;

        BigDecimal frequencia = entrada.frequenciaHoras();
        BigDecimal volume = entrada.volumeMl();
        BigDecimal kcal = entrada.kcalPor100ml();
        BigDecimal proteina = entrada.proteinaPor100ml();

        if (!positivo(frequencia)) {
            motivoDieta = SEM_FREQUENCIA;
        } else if (!positivo(volume)) {
            motivoDieta = SEM_VOLUME;
        } else {
            // "Frequência" é o INTERVALO em horas: a cada 3 h são 8 tomadas.
            vezesDia = VINTE_E_QUATRO.divide(frequencia, CONTA);
            volumeTotal = vezesDia.multiply(volume, CONTA);

            if (kcal == null || proteina == null) {
                motivoDieta = SEM_FORMULA;
            } else {
                // A composição é declarada por 100 ml — daí o /100.
                caloriasTotais = kcal.multiply(volumeTotal, CONTA).divide(CEM, CONTA);
                proteinaTotal = proteina.multiply(volumeTotal, CONTA).divide(CEM, CONTA);

                if (positivo(vet))
                    percCalorico = caloriasTotais.divide(vet, CONTA).multiply(CEM, CONTA);
                if (positivo(proteinaNecessidade))
                    percProteico = proteinaTotal.divide(proteinaNecessidade, CONTA).multiply(CEM, CONTA);
            }
        }

        return new ResultadoPediatrico(
                escala(imc), motivoImc,
                pesoIdade, estaturaIdade, imcIdade, motivoEstado,
                escala(vet), motivoVet,
                escala(proteinaNecessidade), motivoProteina,
                escala(vezesDia), escala(volumeTotal),
                escala(caloriasTotais), escala(proteinaTotal),
                percentual(percCalorico), percentual(percProteico),
                motivoDieta);
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * A regra de três faixas da OMS.
     *
     * <p>Replica {@code CHOOSE(1 + (valor>=P15) + (valor>P85), …)} das células
     * B8, B9 e B11. <b>P15 e P85 são ambos inclusivos na faixa adequada</b> —
     * só {@code > P85} sobe. Trocar o {@code >} por {@code >=} classificaria
     * como "acima do peso" uma criança que está exatamente no P85.
     */
    static FaixaOms faixa(BigDecimal valor, BigDecimal p15, BigDecimal p85) {
        if (valor == null || p15 == null || p85 == null) return null;
        if (valor.compareTo(p15) < 0) return FaixaOms.BAIXA;
        if (valor.compareTo(p85) > 0) return FaixaOms.ALTA;
        return FaixaOms.ADEQUADA;
    }

    /**
     * VET = (89 × peso − 100) + deposição energética da faixa etária.
     *
     * <p>Equação de EER para lactentes das DRIs, 2002 (IOM). Célula B13.
     *
     * @param peso      kg
     * @param deposicao kcal/dia da faixa
     * @return kcal/dia
     */
    static BigDecimal vet(BigDecimal peso, BigDecimal deposicao) {
        return new BigDecimal("89").multiply(peso, CONTA)
                .subtract(CEM, CONTA)
                .add(deposicao, CONTA);
    }

    /**
     * Deposição energética por faixa etária — DRIs, 2002. Tabela M14:N18.
     *
     * @return kcal/dia, ou {@code null} acima de 35 meses, onde a equação não
     *         é definida
     */
    static BigDecimal deposicaoEnergetica(Integer idadeMeses) {
        if (idadeMeses == null || idadeMeses < 0) return null;
        if (idadeMeses <= 3)  return new BigDecimal("175");
        if (idadeMeses <= 6)  return new BigDecimal("56");
        if (idadeMeses <= 12) return new BigDecimal("22");
        if (idadeMeses <= 35) return new BigDecimal("20");
        return null;
    }

    /**
     * Necessidade proteica por faixa etária — DRIs, 2002. Tabela M20:N27.
     *
     * <p><b>Valor fixo em g/dia, não por quilo.</b> AI para 0–6 meses, RDA nas
     * demais. As faixas de 4 a 18 anos existem na planilha mas nenhuma fórmula
     * dela as usa; ficam documentadas em docs/09 §6 e fora daqui — habilitá-las
     * é decisão clínica, não herança de tabela.
     *
     * @return g/dia, ou {@code null} acima de 36 meses
     */
    static BigDecimal necessidadeProteica(Integer idadeMeses) {
        if (idadeMeses == null || idadeMeses < 0) return null;
        if (idadeMeses <= 6)  return new BigDecimal("9.1");
        if (idadeMeses <= 12) return new BigDecimal("11");
        if (idadeMeses <= 36) return new BigDecimal("13");
        return null;
    }

    // Package-private, e não private, porque o acompanhamento diário
    // (AcompanhamentoPediatricoCalculator) usa a MESMA escala e a MESMA régua.
    // Uma segunda cópia delas divergiria — e a régua é a da OMS.

    static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    static BigDecimal escala(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    static BigDecimal percentual(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    /** A precisão das contas intermediárias, compartilhada pelo acompanhamento. */
    static MathContext conta() {
        return CONTA;
    }

    static BigDecimal cem() {
        return CEM;
    }
}
