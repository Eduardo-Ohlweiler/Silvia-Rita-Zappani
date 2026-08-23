package com.nutri.hospitalar.uti.enums;

/**
 * De onde saiu um valor da cascata — e a razão de este enum existir é que a
 * planilha <b>não tem cascata</b>: o peso do paciente está digitado em oito
 * células independentes e a altura em quatro (defeitos 14 e 15 de
 * {@code docs/10} §11).
 *
 * <p>No eroERP o mesmo problema aparece de outra forma: em
 * {@code calculoAntropometria.ts:94} a queda para o peso estimado é ternário
 * ({@code > 0}) e em {@code useCalculoNutricional.ts:118} é {@code ??} (só
 * {@code null}) — então <b>com o peso zerado o IMC usa a estimativa de Chumlea e
 * a meta calórica usa 0, na mesma tela, os dois plausíveis.</b>
 *
 * <p>Aqui o valor não anda sozinho: ele viaja com a sua origem, e a tela a
 * escreve por extenso ao lado do número. Ninguém precisa adivinhar de onde veio
 * o peso que gerou a prescrição.
 */
public enum OrigemValor {

    INFORMADO("informado"),
    ESTIMADO_CHUMLEA("peso estimado · Chumlea 1988"),
    ESTIMADO_JUNG("peso estimado · Jung 2004"),
    ESTIMADO_RABITO("peso estimado · Rabito 2008"),
    ALTURA_ESTIMADA_CHUMLEA("altura estimada · Chumlea 1985"),
    PESO_AJUSTADO("peso ajustado"),
    PESO_IDEAL("peso ideal"),
    DESCONTO_AMPUTACAO("corrigido por amputação"),
    META_POR_FAIXA("da faixa da fase"),
    META_PERSONALIZADA("alvo informado"),
    META_OBESIDADE("protocolo de obesidade"),
    AGUA_LIVRE_ROTULO("água livre do rótulo"),
    AGUA_LIVRE_ESTIMADA("estimada pela densidade");

    private final String descricao;

    OrigemValor(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
