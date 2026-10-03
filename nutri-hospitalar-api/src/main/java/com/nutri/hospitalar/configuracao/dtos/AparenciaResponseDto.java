package com.nutri.hospitalar.configuracao.dtos;

import com.nutri.hospitalar.configuracao.enums.PaletaSistema;

/**
 * O que a rota pública devolve — <b>só</b> a paleta. Nada de data de
 * alteração nem de quem alterou: a rota é lida sem login, e o que ela não
 * expõe não precisa ser protegido.
 */
public record AparenciaResponseDto(PaletaSistema paleta) {
}
