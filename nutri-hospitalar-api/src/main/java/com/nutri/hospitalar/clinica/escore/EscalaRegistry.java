package com.nutri.hospitalar.clinica.escore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * De {@code modelo_ficha.escore_codigo} para a classe que sabe interpretá-lo.
 *
 * <p>Não é um bean do Spring de propósito: as escalas são classes puras, como as
 * de {@code uti/calculo/}, e um mapa estático mantém o teste unitário sem
 * contexto. Acrescentar a terceira escala é uma linha aqui, uma classe ao lado e
 * uma migration de seed — nenhuma alteração em service, DTO ou tela.
 */
public final class EscalaRegistry {

    private static final Map<String, EscalaNutricional> POR_CODIGO =
            Stream.of(new MnaEscala(), new Nrs2002Escala())
                    .collect(Collectors.toMap(EscalaNutricional::codigo,
                            Function.identity(),
                            (a, b) -> a,
                            LinkedHashMap::new));

    private EscalaRegistry() {}

    /**
     * Vazio para modelo sem escala — que é o caso dos três questionários
     * descritivos da fatia 12 — e também para um código que nenhuma classe
     * implementa. O segundo caso não explode: um código órfão numa coluna faria
     * a ficha inteira ficar impossível de abrir, e a promessa deste módulo é a
     * oposta.
     */
    public static Optional<EscalaNutricional> de(String codigo) {
        if (codigo == null || codigo.isBlank()) return Optional.empty();
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }

    public static boolean conhece(String codigo) {
        return de(codigo).isPresent();
    }
}
