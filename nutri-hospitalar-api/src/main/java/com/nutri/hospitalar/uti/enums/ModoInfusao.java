package com.nutri.hospitalar.uti.enums;

import java.math.BigDecimal;

/**
 * Contínua ou intermitente.
 *
 * <p><b>O modo não muda a matemática.</b> Nas duas, {@code VT = volume × tempo};
 * o que muda é só o nome das grandezas na tela — "ml/h × horas" contra
 * "ml/horário × horários". Por isso existe <b>uma</b> classe de dieta, e não
 * duas: a planilha tem duas abas quase idênticas
 * ({@code Contínuo} com 52 linhas e {@code Intermitente} com 50) e é dessa
 * duplicação que saíram cinco dos 22 defeitos.
 *
 * <p>O que o enum carrega é vocabulário e o padrão de tempo — nunca fórmula.
 */
public enum ModoInfusao {

    CONTINUA("Contínua", "horas", "ml/h", new BigDecimal("22")),
    INTERMITENTE("Intermitente", "horários", "ml/horário", null);

    private final String descricao;
    private final String rotuloTempo;
    private final String rotuloVolume;

    /**
     * Tempo sugerido, em horas. 22 h/dia é o padrão de UTI
     * ({@code Contínuo!R1}); no intermitente não há padrão — o número de
     * horários é decisão da prescrição.
     */
    private final BigDecimal tempoPadrao;

    ModoInfusao(String descricao, String rotuloTempo, String rotuloVolume, BigDecimal tempoPadrao) {
        this.descricao = descricao;
        this.rotuloTempo = rotuloTempo;
        this.rotuloVolume = rotuloVolume;
        this.tempoPadrao = tempoPadrao;
    }

    public String getDescricao()    { return descricao; }
    public String getRotuloTempo()  { return rotuloTempo; }
    public String getRotuloVolume() { return rotuloVolume; }
    public BigDecimal getTempoPadrao() { return tempoPadrao; }
}
