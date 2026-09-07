package com.nutri.hospitalar.clinica.enums;

/**
 * O que uma pergunta de modelo de ficha aceita como resposta.
 *
 * <p>São os sete tipos do eroERP, e a lista é fechada de propósito: cada tipo
 * novo é um componente novo na tela, uma regra nova de validação e uma linha
 * nova no documento impresso. Sete cobrem uma anamnese inteira.
 *
 * <p><b>O tipo é copiado para a resposta</b>, e não só declarado no campo. O
 * {@code valor} gravado é sempre {@code TEXT}; é o tipo do <b>retrato</b> que diz
 * se {@code "true"} é um sim/não, se {@code "2026-09-06"} é data e se
 * {@code ["Leite","Ovo"]} é opção múltipla. Sem ele, uma ficha antiga cujo campo
 * mudou de tipo no modelo passaria a ser lida errado.
 */
public enum TipoCampoFicha {

    /** Uma linha. Nome de medicamento, de alergia. */
    TEXTO,

    /** Várias linhas. Queixa, objetivo, descrição livre. */
    TEXTO_LONGO,

    /**
     * Sim / Não — e <b>não informado</b>.
     *
     * <p>Os três estados são obrigatórios. Uma caixa desmarcada não distingue
     * "o paciente disse que não" de "ninguém perguntou", e num prontuário essa é
     * a diferença que importa. Ausência não é valor.
     */
    CHECKBOX,

    /** Dia do calendário. Nunca instante — ver a armadilha do fuso no CLAUDE.md. */
    DATA,

    /** Número com casas decimais. Litros de água, refeições por dia. */
    NUMERO,

    /** Uma opção entre as declaradas em {@code opcoes}. */
    OPCOES,

    /** Zero ou mais opções entre as declaradas em {@code opcoes}. */
    MULTIPLAS_OPCOES;

    /** Os dois tipos que exigem a lista de opções preenchida. */
    public boolean exigeOpcoes() {
        return this == OPCOES || this == MULTIPLAS_OPCOES;
    }
}
