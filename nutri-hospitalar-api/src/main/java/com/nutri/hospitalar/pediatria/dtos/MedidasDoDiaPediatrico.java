package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;

/**
 * O que foi medido e ofertado num dia de acompanhamento pediátrico.
 *
 * <p>Existe pela mesma razão de {@code MedidasDoDia} na UTI: os DTOs de criação e
 * de alteração têm exatamente os mesmos campos, e a alternativa eram dois
 * métodos gêmeos que divergem na primeira vez que alguém acrescenta um campo em
 * um só.
 */
public interface MedidasDoDiaPediatrico {

    BigDecimal pesoKg();
    BigDecimal estaturaCm();

    BigDecimal volPrescrito24h();
    BigDecimal volRecebido24h();

    Integer tomadasPrevistas();
    Integer tomadasAceitas();

    String observacao();
}
