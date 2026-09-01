package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A avaliação que o dia <b>deveria</b> referenciar — sugestão, não vínculo.
 *
 * <p>A tela mostra o que veio e o usuário confirma. Ligar sozinho faria a
 * adequação calórica mudar sem que ninguém tivesse escolhido a referência.
 *
 * <p>Os quatro valores acompanham a sugestão para o usuário poder <b>decidir</b>:
 * "a avaliação de 12/03, peso 9 kg, 880 ml/dia de NAN 2" é informação suficiente
 * para reconhecer se é aquela mesmo.
 *
 * @param aviso quando não há avaliação até a data — e o dia pode ser registrado
 *              assim mesmo. Todos os outros campos vêm nulos nesse caso
 */
public record AvaliacaoPediatricaSugeridaDto(
        UUID id,
        LocalDate dataAvaliacao,
        BigDecimal peso,
        BigDecimal volumeTotal,
        BigDecimal vet,
        BigDecimal proteinaNecessidade,
        String formulaNome,
        String aviso
) {}
