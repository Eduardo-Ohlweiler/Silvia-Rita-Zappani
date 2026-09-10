package com.nutri.hospitalar.clinica.escore;

import com.nutri.hospitalar.clinica.dtos.ClassificacaoEscoreDto;
import com.nutri.hospitalar.clinica.dtos.EscoreDto;
import com.nutri.hospitalar.clinica.dtos.GrupoEscoreDto;
import com.nutri.hospitalar.clinica.escore.SomaPorGrupo.Subtotal;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MNA® — Mini Nutritional Assessment. Especificação em {@code docs/13 §2}.
 *
 * <p>Fonte: Vellas B, Villars H, Abellan G, et al. <i>Overview of the MNA® — Its
 * History and Challenges.</i> J Nutr Health Aging 2006;10:456-465. Rubenstein LZ
 * et al., J Gerontol 2001;56A:M366-377. Guigoz Y, J Nutr Health Aging
 * 2006;10:466-487.
 *
 * <p><b>MNA® é marca registrada da Société des Produits Nestlé SA</b> — ©
 * 1994, revisão 2009 —, de uso clínico livre mediante atribuição. Como o sistema
 * redistribui o instrumento a todo cliente, o crédito é obrigatório, e aparece na
 * descrição do modelo semeado e no rodapé da folha impressa.
 *
 * <p><b>Esta escala não publica conduta.</b> A folha original classifica
 * ("desnutrido", "sob risco de desnutrição", "estado nutricional normal") e não
 * diz o que fazer — então {@code conclusao} vem nula. Escrever uma conduta aqui
 * seria acrescentar ao instrumento, que é justamente o que docs/09 e docs/10
 * chamam de inferência.
 */
public final class MnaEscala implements EscalaNutricional {

    static final String TRIAGEM = "TRIAGEM";
    static final String GLOBAL = "GLOBAL";

    private static final BigDecimal MAX_TRIAGEM = new BigDecimal("14");
    private static final BigDecimal MAX_GLOBAL = new BigDecimal("16");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("30");

    /* Faixas de docs/13 §2.2 e §2.4. Comparação por compareTo: BigDecimal.equals
       distingue 9 de 9.0, e a escala do subtotal não é assunto da faixa. */
    private static final BigDecimal TRIAGEM_NORMAL = new BigDecimal("12");
    private static final BigDecimal TRIAGEM_RISCO = new BigDecimal("8");
    private static final BigDecimal TOTAL_NORMAL = new BigDecimal("24");
    private static final BigDecimal TOTAL_RISCO = new BigDecimal("17");

    @Override public String codigo() { return "MNA"; }

    @Override public String nome() { return "MNA® — Mini Nutritional Assessment"; }

    @Override public String referencia() {
        return "Vellas et al., J Nutr Health Aging 2006 · MNA® © Société des Produits Nestlé SA, "
                + "1994, revisão 2009";
    }

    @Override
    public Map<String, BigDecimal> maximoPorGrupo() {
        Map<String, BigDecimal> maximos = new LinkedHashMap<>();
        maximos.put(TRIAGEM, MAX_TRIAGEM);
        maximos.put(GLOBAL, MAX_GLOBAL);
        return maximos;
    }

    @Override
    public EscoreDto avaliar(List<ItemRespondido> itens, Integer idadeAnos) {
        Map<String, Subtotal> subtotais = SomaPorGrupo.de(itens);
        Subtotal triagem = subtotais.getOrDefault(TRIAGEM, SomaPorGrupo.vazio());
        Subtotal global = subtotais.getOrDefault(GLOBAL, SomaPorGrupo.vazio());

        List<GrupoEscoreDto> grupos = List.of(
                grupoTriagem(triagem),
                grupoGlobal(global));

        return montarTotal(triagem, global, grupos);
    }

    /**
     * A triagem <b>classifica sozinha</b>: ela é o instrumento inteiro na versão
     * abreviada (MNA-SF), e as faixas de 12-14 / 8-11 / 0-7 são publicadas para
     * ela isoladamente.
     */
    private GrupoEscoreDto grupoTriagem(Subtotal triagem) {
        if (triagem.completo()) {
            return new GrupoEscoreDto(TRIAGEM, "Triagem", triagem.soma(), MAX_TRIAGEM,
                    faixaTriagem(triagem.soma()), null, List.of());
        }
        return new GrupoEscoreDto(TRIAGEM, "Triagem", null, MAX_TRIAGEM, null,
                EscalaNutricional.faltamResponder(triagem),
                EscalaNutricional.pendentesVisiveis(triagem));
    }

    /**
     * A avaliação global <b>não</b> classifica sozinha — não há faixa publicada
     * para os 16 pontos isolados, e inventar uma seria criar um instrumento novo.
     * Ela existe para compor o total.
     */
    private GrupoEscoreDto grupoGlobal(Subtotal global) {
        if (global.completo()) {
            return new GrupoEscoreDto(GLOBAL, "Avaliação global", global.soma(), MAX_GLOBAL,
                    null, null, List.of());
        }
        return new GrupoEscoreDto(GLOBAL, "Avaliação global", null, MAX_GLOBAL, null,
                EscalaNutricional.faltamResponder(global),
                EscalaNutricional.pendentesVisiveis(global));
    }

    /**
     * Os quatro caminhos, com {@code else} final.
     *
     * <p>Cadeia de {@code else if} sem {@code else} é traço mudo esperando
     * acontecer — foi assim que o {@code CalculoPediatricoCalculator} deixou três
     * campos sem motivo. Aqui todo caminho que não produz total produz frase.
     */
    private EscoreDto montarTotal(Subtotal triagem, Subtotal global, List<GrupoEscoreDto> grupos) {
        BigDecimal total = null;
        ClassificacaoEscoreDto classificacao = null;
        String motivo;

        if (triagem.completo() && global.completo()) {
            total = triagem.soma().add(global.soma());
            classificacao = faixaTotal(total);
            motivo = null;
        } else if (triagem.completo()) {
            motivo = "O escore total exige as perguntas G a R, da avaliação global.";
        } else if (global.completo()) {
            motivo = "O escore total exige as perguntas A a F, da triagem.";
        } else {
            motivo = "O escore total exige a triagem e a avaliação global.";
        }

        return new EscoreDto(codigo(), nome(), referencia(), grupos,
                null, null,
                total, MAX_TOTAL, classificacao, null, motivo);
    }

    /** docs/13 §2.2: 12 a 14 normal · 8 a 11 sob risco · 0 a 7 desnutrido. */
    private ClassificacaoEscoreDto faixaTriagem(BigDecimal soma) {
        if (soma.compareTo(TRIAGEM_NORMAL) >= 0)
            return ClassificacaoEscoreDto.adequada("Estado nutricional normal");
        if (soma.compareTo(TRIAGEM_RISCO) >= 0)
            return ClassificacaoEscoreDto.atencao("Sob risco de desnutrição");
        return ClassificacaoEscoreDto.critica("Desnutrido");
    }

    /** docs/13 §2.4: 24 a 30 normal · 17 a 23,5 sob risco · menos de 17 desnutrido. */
    private ClassificacaoEscoreDto faixaTotal(BigDecimal soma) {
        if (soma.compareTo(TOTAL_NORMAL) >= 0)
            return ClassificacaoEscoreDto.adequada("Estado nutricional normal");
        if (soma.compareTo(TOTAL_RISCO) >= 0)
            return ClassificacaoEscoreDto.atencao("Sob risco de desnutrição");
        return ClassificacaoEscoreDto.critica("Desnutrido");
    }
}
