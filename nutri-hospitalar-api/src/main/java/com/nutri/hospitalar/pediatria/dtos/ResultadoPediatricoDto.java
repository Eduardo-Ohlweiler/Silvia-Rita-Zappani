package com.nutri.hospitalar.pediatria.dtos;

/**
 * O resultado completo de um cálculo pediátrico, em três blocos que
 * correspondem às abas da tela.
 *
 * <p>É a mesma forma devolvida por {@code POST /calcular} e pela avaliação
 * salva, para a tela renderizar o resultado com um componente só, venha o
 * número de onde vier.
 *
 * <p><b>Numa avaliação salva os motivos vêm nulos.</b> Eles explicam uma
 * ausência no momento em que se digita ("acima de 35 meses"); o registro
 * gravado mostra o que foi calculado, e o motivo reaparece assim que o usuário
 * mexe em qualquer entrada e a tela volta a chamar o {@code /calcular}.
 */
public record ResultadoPediatricoDto(
        EstadoNutricionalDto estadoNutricional,
        NecessidadesDto necessidades,
        DietaCalculadaDto dieta
) {}
