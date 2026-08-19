package com.nutri.hospitalar.pessoa.dtos;

import com.nutri.hospitalar.pessoa.enums.Sexo;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Item de combo. Leva o documento junto porque homônimo é comum — "Maria
 * Silva" sozinha não identifica ninguém numa lista de pacientes.
 *
 * <p>{@code dataNascimento} e {@code sexo} viajam junto para a tela de cálculo
 * preencher idade e sexo ao escolher o paciente, sem uma segunda ida ao
 * servidor. Ambos podem vir nulos — cadastro incompleto ou pessoa jurídica —
 * e a tela aceita que o profissional complete na hora.
 */
public record PessoaSelectDto(
        UUID id,
        String nome,
        String documento,
        LocalDate dataNascimento,
        Sexo sexo
) {}
