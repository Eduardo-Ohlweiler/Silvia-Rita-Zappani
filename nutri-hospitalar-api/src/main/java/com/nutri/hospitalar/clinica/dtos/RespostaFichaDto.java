package com.nutri.hospitalar.clinica.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Uma resposta, na ida.
 *
 * <p>Só chega o <b>campo</b> e o <b>valor</b>: o retrato da pergunta é copiado
 * pelo servidor a partir do campo vivo, e nunca vem do cliente. Aceitar rótulo
 * do front abriria caminho para uma ficha afirmar ter perguntado o que o modelo
 * não perguntava.
 *
 * @param valor sempre texto, qualquer que seja o tipo. {@code CHECKBOX} manda
 *              {@code "true"}, {@code "false"} ou <b>nulo</b>, e nulo é "não
 *              informado" — que não é a mesma coisa que "não".
 *              {@code MULTIPLAS_OPCOES} manda um JSON array.
 */
public record RespostaFichaDto(

        @NotNull(message = "Informe a qual pergunta a resposta pertence")
        UUID campoId,

        String valor
) {}
