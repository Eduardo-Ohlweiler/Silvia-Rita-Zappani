package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.enums.EtniaChumlea;

import java.math.BigDecimal;

import static com.nutri.hospitalar.uti.calculo.UtiMatematica.CONTA;

/**
 * As três equações de peso estimado, para o paciente que não pode ser pesado —
 * acamado, em ventilação, sem balança de leito.
 *
 * <p>Especificação: {@code docs/10-calculos-uti-adulto.md} §2.2 a §2.4, extraída
 * célula a célula de {@code Facilita Nutri na UTI}. Divergiu da planilha, este
 * código está errado — não o contrário.
 *
 * <p>Classe pura: sem estado, sem Spring, sem banco. Todas as medidas em
 * <b>centímetros</b>, idade em <b>anos</b>, resultado em <b>quilos</b>, sem
 * arredondamento — quem arredonda é a saída, uma vez só.
 *
 * <p>A estimativa de <i>altura</i> (Chumlea 1985) não mora aqui: ela produz um
 * {@link com.nutri.hospitalar.uti.calculo.cascata.Altura} e vive em
 * {@link AntropometriaCalculator}.
 */
public final class EstimativaPesoCalculator {

    private EstimativaPesoCalculator() {}

    /** A partir daqui o paciente entra na faixa de idoso das equações. */
    static final int IDADE_IDOSO = 60;

    /**
     * Peso estimado — Chumlea et al. (1988), a partir da altura do joelho e da
     * circunferência do braço. Oito ramos: sexo × etnia × faixa de idade.
     *
     * <p>Confere com {@code Estimativas!B13:B20}: AJ 53 cm, CB 25 cm, 59 anos →
     * 54,75 · 59,24 · 52,55 · 55,61 · 57,74 · 59,26 · 57,49 · 59,78 kg, nessa
     * ordem.
     *
     * <p><b>Os 60 anos exatos não caem em faixa nenhuma na planilha</b> — os
     * rótulos são "19-59 ANOS" e "&gt;60 ANOS". Adotamos <b>≥60 = idoso</b>, por
     * coerência com o Estatuto do Idoso (Lei 10.741/2003). É convenção nossa,
     * não leitura da fonte, e tem teste em 59, 60 e 61.
     *
     * <p><b>Pendência de documentação:</b> a fonte primária exata deste conjunto
     * de oito ramos não foi identificada — a literatura primária de Chumlea 1988
     * usa quatro medidas, e este conjunto usa duas. O que o torna auditável é
     * que os valores em cache da planilha conferem com as equações dela. Ver
     * {@code docs/10} §2.2 e §13.
     *
     * @param alturaJoelhoCm altura do joelho, em cm
     * @param circBracoCm    circunferência do braço, em cm
     * @param idadeAnos      idade em anos completos
     * @return peso em kg, ou {@code null} se faltar qualquer entrada
     */
    public static BigDecimal chumlea1988(BigDecimal alturaJoelhoCm, BigDecimal circBracoCm,
                                         Integer idadeAnos, Sexo sexo, EtniaChumlea etnia) {
        if (alturaJoelhoCm == null || circBracoCm == null
                || idadeAnos == null || sexo == null || etnia == null) return null;

        boolean idoso = idadeAnos >= IDADE_IDOSO;

        String[] coeficientes = switch (sexo) {
            case MASCULINO -> switch (etnia) {
                case BRANCA -> idoso ? new String[]{"1.10", "3.07", "75.81"}
                                     : new String[]{"1.19", "3.14", "86.82"};
                case NEGRA  -> idoso ? new String[]{"0.44", "2.86", "39.21"}
                                     : new String[]{"1.09", "3.14", "83.72"};
            };
            case FEMININO -> switch (etnia) {
                case BRANCA -> idoso ? new String[]{"1.09", "2.68", "65.51"}
                                     : new String[]{"1.01", "2.81", "66.04"};
                case NEGRA  -> idoso ? new String[]{"1.50", "2.58", "84.22"}
                                     : new String[]{"1.24", "2.97", "82.48"};
            };
        };

        return alturaJoelhoCm.multiply(new BigDecimal(coeficientes[0]), CONTA)
                .add(circBracoCm.multiply(new BigDecimal(coeficientes[1]), CONTA), CONTA)
                .subtract(new BigDecimal(coeficientes[2]), CONTA);
    }

    /**
     * Peso estimado — Jung et al. (2004), a partir da altura do joelho, da
     * circunferência do braço e da idade.
     *
     * <pre>
     * Homem : (AJ × 0,928) + (CB × 2,508) − (idade × 0,144) − 42,543
     * Mulher: (AJ × 0,826) + (CB × 2,116) − (idade × 0,133) − 31,486
     * </pre>
     *
     * <p>Confere com {@code Estimativas!B24}/{@code B25}: AJ 53, CB 25, 59 anos →
     * 60,845 (H) e 57,345 (M).
     */
    public static BigDecimal jung2004(BigDecimal alturaJoelhoCm, BigDecimal circBracoCm,
                                      Integer idadeAnos, Sexo sexo) {
        if (alturaJoelhoCm == null || circBracoCm == null
                || idadeAnos == null || sexo == null) return null;

        String[] c = sexo == Sexo.MASCULINO
                ? new String[]{"0.928", "2.508", "0.144", "42.543"}
                : new String[]{"0.826", "2.116", "0.133", "31.486"};

        return alturaJoelhoCm.multiply(new BigDecimal(c[0]), CONTA)
                .add(circBracoCm.multiply(new BigDecimal(c[1]), CONTA), CONTA)
                .subtract(new BigDecimal(idadeAnos).multiply(new BigDecimal(c[2]), CONTA), CONTA)
                .subtract(new BigDecimal(c[3]), CONTA);
    }

    /**
     * Peso estimado — Rabito et al. (2008), a partir de três circunferências.
     *
     * <pre>
     * Peso = 0,5759×CB + 0,5263×CA + 1,2452×CP − 4,8689×sexo − 32,9241
     *        onde sexo: masculino = 1, feminino = 2
     * </pre>
     *
     * <p>Confere com {@code Estimativas!B28}/{@code B29}: CB 25, CA 90, CP 34 →
     * 66,3083 (H) e 61,4394 (M).
     *
     * @param circBracoCm       circunferência do braço, em cm
     * @param circAbdominalCm   circunferência abdominal, em cm
     * @param circPanturrilhaCm circunferência da panturrilha, em cm
     */
    public static BigDecimal rabito2008(BigDecimal circBracoCm, BigDecimal circAbdominalCm,
                                        BigDecimal circPanturrilhaCm, Sexo sexo) {
        if (circBracoCm == null || circAbdominalCm == null
                || circPanturrilhaCm == null || sexo == null) return null;

        BigDecimal codigoSexo = sexo == Sexo.MASCULINO ? BigDecimal.ONE : new BigDecimal("2");

        return circBracoCm.multiply(new BigDecimal("0.5759"), CONTA)
                .add(circAbdominalCm.multiply(new BigDecimal("0.5263"), CONTA), CONTA)
                .add(circPanturrilhaCm.multiply(new BigDecimal("1.2452"), CONTA), CONTA)
                .subtract(codigoSexo.multiply(new BigDecimal("4.8689"), CONTA), CONTA)
                .subtract(new BigDecimal("32.9241"), CONTA);
    }
}
