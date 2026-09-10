package com.nutri.hospitalar.clinica.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Os pontos de uma pergunta pontuada — irmão de {@link OpcoesJson}, e com a
 * mesma promessa: <b>ler nunca explode</b>.
 *
 * <p>É um JSON array de decimais <b>paralelo por índice</b> a {@code opcoes}: a
 * terceira opção vale o terceiro ponto. Guardar o par como duas listas alinhadas,
 * e não como uma lista de objetos, mantém {@code opcoes} exatamente como está —
 * nenhuma das cinco telas que já o leem precisou mudar.
 *
 * <p><b>{@code BigDecimal}, nunca {@code double}</b> — e não porque a MNA
 * quebraria em ponto flutuante: os meios-pontos dela (0,5) são exatamente
 * representáveis em binário, e somá-los em {@code double} não perde nada hoje. O
 * motivo é a próxima escala. {@code 0.1 + 0.2 != 0.3}, e a faixa intermediária da
 * MNA já é decidida por uma comparação em <b>23,5</b>: um instrumento que pontue
 * em décimos trocaria "sob risco de desnutrição" por "desnutrido" num prontuário,
 * em silêncio. Depender de "0,5 é exato" é construir sobre uma propriedade que a
 * próxima migration de seed revoga sem ninguém perceber.
 */
public final class PontosJson {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<BigDecimal>> LISTA = new TypeReference<>() {};

    private PontosJson() {}

    /** Texto da coluna → lista. Nulo, vazio ou inválido viram lista vazia. */
    public static List<BigDecimal> paraLista(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<BigDecimal> lida = JSON.readValue(json, LISTA);
            return lida == null || lida.contains(null) ? List.of() : lida;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Lista → texto da coluna. Lista vazia vira nulo, e não {@code "[]"}. */
    public static String paraTexto(List<BigDecimal> pontos) {
        if (pontos == null || pontos.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(pontos);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível serializar os pontos", e);
        }
    }

    /**
     * Quanto vale a resposta {@code valor} nesta pergunta.
     *
     * <p>Devolve {@code null} quando a pergunta não pontua, quando não foi
     * respondida, ou quando a resposta <b>não está entre as opções</b> — e este
     * último caso é deliberado: um valor órfão não pode valer zero em silêncio,
     * porque zero é um ponto legítimo em quase todo item das duas escalas.
     * Devolvendo nulo, a pergunta conta como <b>não respondida</b> e aparece na
     * frase "faltam responder", que é visível. Ponto perdido calado é o que este
     * projeto chama de traço mudo.
     */
    public static BigDecimal pontoDe(String opcoesJson, String pontosJson, String valor) {
        if (valor == null || pontosJson == null) return null;

        List<String> opcoes = OpcoesJson.paraLista(opcoesJson);
        List<BigDecimal> pontos = paraLista(pontosJson);

        /* Cardinalidade diferente é catálogo inconsistente: não se adivinha qual
           ponto pertence a qual opção. O service recusa isso na gravação do
           modelo; aqui só não se inventa. */
        if (opcoes.size() != pontos.size()) return null;

        int indice = opcoes.indexOf(valor);
        return indice < 0 ? null : pontos.get(indice);
    }
}
