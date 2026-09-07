package com.nutri.hospitalar.clinica.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * As opções de uma pergunta viajam como {@code List<String>} e são guardadas
 * como um JSON array em {@code TEXT} — é o formato do eroERP, e mantê-lo evita
 * uma quinta tabela para uma lista de quatro palavras.
 *
 * <p><b>Ler nunca explode.</b> JSON inválido devolve lista vazia em vez de
 * exceção: o valor vem de uma coluna que pode ter sido escrita à mão num
 * {@code UPDATE} de manutenção, e uma ficha inteira não pode ficar impossível de
 * abrir por causa de uma vírgula numa pergunta.
 */
public final class OpcoesJson {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> LISTA = new TypeReference<>() {};

    private OpcoesJson() {}

    /** Texto da coluna → lista. Nulo, vazio ou inválido viram lista vazia. */
    public static List<String> paraLista(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> lida = JSON.readValue(json, LISTA);
            return lida == null ? List.of() : lida;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Lista → texto da coluna. Lista vazia vira nulo, e não {@code "[]"}. */
    public static String paraTexto(List<String> opcoes) {
        if (opcoes == null || opcoes.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(opcoes);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível serializar as opções", e);
        }
    }
}
