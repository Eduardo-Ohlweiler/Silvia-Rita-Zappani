package com.nutri.hospitalar.clinica.dtos;

import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Uma pergunta do modelo, na ida e na volta.
 *
 * <p>O {@code id} ausente significa <b>pergunta nova</b>; presente, pergunta a
 * atualizar. A que não vier na lista foi removida — a lista é o estado completo,
 * como em {@code ContatoService} (docs/08 §3.3).
 *
 * <p><b>Não há campo {@code ordem}.</b> A ordem é a posição na lista, e é o
 * service quem a reatribui: ordem digitada à mão produz duas perguntas com o
 * número 3 e uma tela que decide o desempate sozinha.
 *
 * @param opcoes lista de opções, obrigatória nos dois tipos de opção e recusada
 *               nos outros cinco — opção pendurada em campo de texto é lixo que
 *               reaparece se o tipo mudar.
 */
public record CampoFichaDto(

        UUID id,

        @Size(max = 200, message = "A seção deve ter no máximo 200 caracteres")
        String secao,

        @NotBlank(message = "Informe o texto da pergunta")
        @Size(max = 300, message = "A pergunta deve ter no máximo 300 caracteres")
        String rotulo,

        @NotNull(message = "Informe o tipo da pergunta")
        TipoCampoFicha tipo,

        List<@NotBlank(message = "Opção em branco") @Size(max = 200) String> opcoes,

        Boolean obrigatorio,

        Boolean ativo
) {}
