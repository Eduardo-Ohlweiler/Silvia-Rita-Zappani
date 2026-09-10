package com.nutri.hospitalar.clinica.escore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A aritmética, e <b>só</b> a aritmética.
 *
 * <p>Esta classe soma pontos por grupo e anota quem ficou de fora. Ela não
 * conhece faixa, porta, corte nem idade — isso é conhecimento clínico com
 * publicação atrás, e mora em cada {@link EscalaNutricional}. A divisão é o que
 * permite a terceira escala custar uma migration e uma classe pequena.
 */
public final class SomaPorGrupo {

    private SomaPorGrupo() {}

    /**
     * @param soma        a soma dos pontos do grupo. Zero num grupo que não
     *                    pontua — e por isso quem lê chama {@link #completo()}
     *                    antes de usar este número
     * @param semResposta os rótulos das perguntas pendentes, na ordem em que se
     *                    responde
     * @param total       quantas perguntas o grupo tem
     */
    public record Subtotal(BigDecimal soma, List<String> semResposta, int total) {

        /**
         * <b>Bloco ausente nunca é completo.</b>
         *
         * <p>Sem o {@code total > 0} um grupo que o modelo não tem passaria por
         * "tudo respondido" e somaria zero — e uma MNA sem as perguntas G a R
         * publicaria a triagem sozinha como escore total, classificada pelas
         * faixas de 30. Nove pontos viram "Desnutrido" ali, com cara de
         * resultado. A guarda do service impede que um modelo assim seja
         * gravado; esta impede que ele seja <b>somado</b>, que é a defesa que não
         * depende da outra existir.
         */
        public boolean completo() {
            return total > 0 && semResposta.isEmpty();
        }

        /** O modelo não tem nenhuma pergunta deste bloco. */
        public boolean ausente() {
            return total == 0;
        }

        /** Ninguém respondeu nada ainda — formulário recém-aberto. */
        public boolean intocado() {
            return semResposta.size() == total;
        }
    }

    /**
     * Agrupa os itens pelo {@code grupo} e soma cada bloco.
     *
     * <p>{@code LinkedHashMap} porque a ordem dos grupos é a ordem em que se
     * responde, e é ela que a tela e o papel usam.
     */
    public static Map<String, Subtotal> de(List<ItemRespondido> itens) {
        Map<String, List<ItemRespondido>> porGrupo = new LinkedHashMap<>();
        for (ItemRespondido item : itens) {
            if (item.pontuavel()) {
                porGrupo.computeIfAbsent(item.grupo(), g -> new ArrayList<>()).add(item);
            }
        }

        Map<String, Subtotal> subtotais = new LinkedHashMap<>();
        porGrupo.forEach((grupo, doGrupo) -> subtotais.put(grupo, somar(doGrupo)));
        return subtotais;
    }

    /**
     * Grupo vazio devolve {@link Subtotal} zerado e <b>completo</b> — não
     * existe no modelo, então não há o que cobrar. Quem decide se a ausência do
     * grupo importa é a escala.
     */
    public static Subtotal vazio() {
        return new Subtotal(escala1(BigDecimal.ZERO), List.of(), 0);
    }

    private static Subtotal somar(List<ItemRespondido> doGrupo) {
        BigDecimal soma = BigDecimal.ZERO;
        List<String> pendentes = new ArrayList<>();

        for (ItemRespondido item : doGrupo) {
            if (item.pontos() != null) soma = soma.add(item.pontos());
            if (item.pendente()) pendentes.add(item.rotulo());
        }

        return new Subtotal(escala1(soma), List.copyOf(pendentes), doGrupo.size());
    }

    /**
     * Escala fixa em uma casa, para {@code 9} e {@code 9,0} serem o mesmo número
     * no JSON e na folha. Nenhum ponto das duas escalas tem mais de uma casa, e
     * por isso o arredondamento nunca chega a arredondar nada — ele está aqui
     * para não explodir num catálogo que alguém editou à mão, e não para
     * corrigir aritmética.
     */
    private static BigDecimal escala1(BigDecimal valor) {
        return valor.setScale(1, RoundingMode.HALF_UP);
    }
}
