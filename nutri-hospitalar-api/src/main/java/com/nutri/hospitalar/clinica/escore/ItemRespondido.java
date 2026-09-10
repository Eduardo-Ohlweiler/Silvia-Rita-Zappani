package com.nutri.hospitalar.clinica.escore;

import com.nutri.hospitalar.clinica.entity.CampoFicha;
import com.nutri.hospitalar.clinica.mapper.PontosJson;

import java.math.BigDecimal;

/**
 * Uma pergunta e o que se sabe dela para efeito de escore.
 *
 * <p>É a fronteira entre o dado e a regra: daqui para dentro nenhuma escala sabe
 * o que é {@code CampoFicha}, {@code opcoes} ou JSON — ela vê grupo, rótulo,
 * pontos e valor. É o que permite a mesma classe servir à ficha em preenchimento
 * e à ficha sendo gravada, sem duas montagens do mesmo número.
 *
 * @param pontuada   <b>o catálogo declara pontos para esta pergunta</b>. Vem do
 *                   catálogo, e não da resposta: é o que distingue "pergunta que
 *                   pontua e ainda não foi respondida" de "pergunta que não
 *                   pontua" — a pré-triagem da NRS-2002 é do segundo tipo, e é
 *                   uma porta, não uma soma
 * @param pontos     quanto esta resposta vale. Nulo quando a pergunta não pontua
 *                   ou quando não foi resolvida
 * @param respondida distinta de {@code pontos != null}: a pré-triagem é
 *                   respondida e não pontua
 */
public record ItemRespondido(
        String grupo,
        String rotulo,
        boolean pontuada,
        BigDecimal pontos,
        boolean respondida,
        String valor
) {

    /**
     * Monta o item a partir da pergunta do modelo e do valor respondido.
     *
     * <p>É aqui, e só aqui, que {@code opcoes} e {@code pontos} são casados por
     * índice — ver {@link PontosJson#pontoDe}.
     */
    public static ItemRespondido de(CampoFicha campo, String valor) {
        String limpo = valor == null || valor.isBlank() ? null : valor;
        return new ItemRespondido(
                campo.getGrupoEscore(),
                campo.getRotulo(),
                campo.getPontos() != null && !campo.getPontos().isBlank(),
                PontosJson.pontoDe(campo.getOpcoes(), campo.getPontos(), limpo),
                limpo != null,
                limpo);
    }

    /** Participa de algum bloco de pontuação. */
    public boolean pontuavel() {
        return grupo != null;
    }

    /**
     * Ainda falta esta pergunta para o grupo fechar.
     *
     * <p>Pergunta pontuada só está resolvida quando <b>vale um número</b>.
     * Respondida com um valor fora das opções devolve {@code pontos} nulo (ver
     * {@code PontosJson.pontoDe}) e continua pendente — aparecendo na frase
     * "faltam responder", em vez de valer zero em silêncio. <b>Zero é ponto
     * legítimo em quase todo item das duas escalas</b>, e por isso não pode ser
     * o valor de quem não respondeu.
     */
    public boolean pendente() {
        return pontuada ? pontos == null : !respondida;
    }
}
