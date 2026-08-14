package com.nutri.hospitalar.pessoa.dtos;

import java.util.UUID;

/**
 * Item de combo. Leva o documento junto porque homônimo é comum — "Maria
 * Silva" sozinha não identifica ninguém numa lista de pacientes.
 */
public record PessoaSelectDto(
        UUID id,
        String nome,
        String documento
) {}
