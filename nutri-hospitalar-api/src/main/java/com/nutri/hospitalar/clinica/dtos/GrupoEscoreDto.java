package com.nutri.hospitalar.clinica.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Um bloco de pontuação de uma escala — a triagem da MNA, a etapa 2 da NRS-2002.
 *
 * @param subtotal              <b>nulo enquanto o grupo estiver incompleto</b>.
 *                              Numa escala, soma parcial não é escore menor: é
 *                              escore errado. Cinco perguntas em branco fazem a
 *                              MNA somar 12 e parecer "sob risco" num paciente
 *                              que pode ser normal
 * @param maximo                nulo no grupo que não pontua — a pré-triagem da
 *                              NRS-2002 é uma porta, não uma soma
 * @param classificacao         só nos grupos que têm faixa publicada. A avaliação
 *                              global da MNA não tem, e por isso não inventa uma
 * @param motivoAusencia        por que o {@code subtotal} está nulo
 * @param perguntasSemResposta  os rótulos inteiros, para a tela poder listá-los
 *                              sem reconstruir nada a partir da frase
 * @param naoSeAplica           a <b>escala dispensou este bloco</b>: a pré-triagem
 *                              da NRS-2002 não encontrou critério, e as etapas 2 e
 *                              3 não são aplicadas (Kondrup 2003, docs/13 §3.2).
 *                              É diferente de incompleto — não falta responder,
 *                              não há o que responder —, e é por isso que o
 *                              formulário desabilita a seção em vez de cobrá-la
 *
 * <p><b>Por que a negativa, e por que {@code Boolean}.</b> Este record é
 * congelado em {@code ficha_anamnese.escore_json}, e toda ficha gravada antes
 * desta mudança tem JSON <b>sem</b> o campo. Um {@code boolean} primitivo
 * desserializaria como {@code false}; se o campo se chamasse {@code aplicavel},
 * esse {@code false} marcaria todo bloco de toda ficha antiga como dispensado.
 * Pela negativa, ausente é o mesmo que "aplica-se", que é o que elas são.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrupoEscoreDto(
        String grupo,
        String rotulo,
        BigDecimal subtotal,
        BigDecimal maximo,
        ClassificacaoEscoreDto classificacao,
        String motivoAusencia,
        List<String> perguntasSemResposta,
        Boolean naoSeAplica
) {

    /** O caso comum: o bloco vale, e o {@code naoSeAplica} nem precisa ser dito. */
    public GrupoEscoreDto(String grupo, String rotulo, BigDecimal subtotal, BigDecimal maximo,
                          ClassificacaoEscoreDto classificacao, String motivoAusencia,
                          List<String> perguntasSemResposta) {
        this(grupo, rotulo, subtotal, maximo, classificacao, motivoAusencia,
                perguntasSemResposta, null);
    }

    /** Nulo é "aplica-se" — ver a nota do record sobre as fichas já gravadas. */
    public boolean dispensado() {
        return Boolean.TRUE.equals(naoSeAplica);
    }
}
