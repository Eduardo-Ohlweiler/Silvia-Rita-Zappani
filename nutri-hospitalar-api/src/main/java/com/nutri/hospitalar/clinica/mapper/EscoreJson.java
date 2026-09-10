package com.nutri.hospitalar.clinica.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutri.hospitalar.clinica.dtos.EscoreDto;

/**
 * O escore congelado, indo e voltando da coluna {@code escore_json}.
 *
 * <p><b>Ler nunca explode</b> — terceira classe deste módulo com essa promessa,
 * e aqui ela vale mais que nas outras duas: um escore ilegível não pode tornar
 * uma ficha de anamnese inteira impossível de abrir. O prontuário é o retrato das
 * respostas; o escore é uma leitura delas. Se a leitura se perder, a ficha
 * continua.
 *
 * <p>O {@code EscoreDto} e os seus dois records aninhados declaram
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} pela mesma razão pelo
 * outro lado: uma ficha gravada hoje precisa continuar abrindo depois de o DTO
 * ganhar um campo.
 */
public final class EscoreJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private EscoreJson() {}

    /** Nulo para modelo sem escala — não é erro, é ausência de escore. */
    public static String paraTexto(EscoreDto escore) {
        if (escore == null) return null;
        try {
            return JSON.writeValueAsString(escore);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível serializar o escore", e);
        }
    }

    /** Nulo, vazio ou ilegível viram nulo — a ficha abre sem o painel de escore. */
    public static EscoreDto paraDto(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JSON.readValue(json, EscoreDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}
