package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.SuporteVentilatorio;

import java.math.BigDecimal;

/**
 * O que foi medido num dia de acompanhamento.
 *
 * <p>Existe porque {@link RegistroDiarioUtiCreateDto} e
 * {@link RegistroDiarioUtiUpdateDto} têm exatamente os mesmos campos de medida —
 * e a alternativa era um método de 27 parâmetros, ou dois métodos gêmeos que
 * divergem na primeira vez que alguém acrescenta um campo em um só.
 *
 * <p>Records implementam interface sem cerimônia: os componentes já são os
 * acessores.
 */
public interface MedidasDoDia {

    String dieta();
    BigDecimal volPrescrito24h();
    BigDecimal volRecebido24h();

    BigDecimal mg();
    BigDecimal k();
    BigDecimal na();
    BigDecimal lactato();
    BigDecimal pcr();
    BigDecimal ph();
    BigDecimal pco2();
    BigDecimal hco3();
    BigDecimal hgt();

    SuporteVentilatorio suporteVentilatorio();
    BigDecimal fio2Perc();
    BigDecimal paSistolica();
    BigDecimal paDiastolica();
    BigDecimal balancoHidricoMl();
    BigDecimal diureseMl();
    String evacuacao();

    BigDecimal cafeManha();
    BigDecimal lancheManha();
    BigDecimal almoco();
    BigDecimal lancheTarde();
    BigDecimal jantar();
    BigDecimal ceia();

    String observacao();
}
