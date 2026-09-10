package com.nutri.hospitalar.clinica.escore;

import com.nutri.hospitalar.clinica.dtos.EscoreDto;
import com.nutri.hospitalar.clinica.escore.SomaPorGrupo.Subtotal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Uma escala nutricional pontuada — a <b>regra</b> de um instrumento publicado.
 *
 * <p>Esta é a metade em código da divisão que a fatia 13 fez (docs/13 §1.2): o
 * <b>ponto de cada opção é dado</b>, no banco, porque são dezenas de números e
 * porque assim eles entram no retrato da resposta; a <b>porta, a faixa, o corte,
 * o ajuste por idade e a frase da conclusão são código</b>, aqui, porque são
 * conhecimento clínico com publicação atrás — o mesmo tipo que docs/09 e docs/10
 * exigiram documentar antes de virar linha.
 *
 * <p><b>A implementação trabalha sobre subtotal de grupo, e nunca procura uma
 * pergunta pelo rótulo.</b> Se procurasse, clonar o modelo e reescrever um
 * enunciado quebraria o casamento em silêncio — e o silêncio é o defeito que
 * este módulo inteiro existe para evitar.
 */
public interface EscalaNutricional {

    /** O código gravado em {@code modelo_ficha.escore_codigo}. */
    String codigo();

    String nome();

    /** A procedência, que acompanha o número na tela e no papel. */
    String referencia();

    EscoreDto avaliar(List<ItemRespondido> itens, Integer idadeAnos);

    /**
     * Quanto <b>tem</b> de valer cada bloco, somando o maior ponto de cada
     * pergunta dele. Na MNA: triagem 14, avaliação global 16.
     *
     * <p>Existe para o service <b>rodar o limite contra os dados</b> antes de
     * gravar um modelo com escala — a mesma lição do Codex contra o catálogo de
     * fórmulas lácteas, onde a faixa "óbvia" reprovava três das dez que o próprio
     * sistema distribui.
     *
     * <p>É o que torna o clone seguro. Sem esta checagem, clonar a MNA e apagar
     * dez perguntas produziria um total máximo de 8 classificado pelas faixas de
     * 30: "desnutrido" para quem respondeu tudo. Reescrever o enunciado de uma
     * pergunta continua permitido — e é o que um cliente legitimamente quer —,
     * porque isso não move a aritmética.
     *
     * <p><b>A ordem importa</b>, e por isso o mapa é ordenado e não um
     * {@code Map.of}: é ela que decide qual bloco a mensagem de erro cita
     * primeiro. Com iteração indefinida, a mesma edição errada produziria uma
     * queixa diferente a cada tentativa — e quem lê concluiria que o sistema
     * está confuso, não a edição.
     */
    Map<String, BigDecimal> maximoPorGrupo();

    /**
     * "Faltam responder: A. Nos últimos três meses…, F. Índice de massa… e mais 2."
     *
     * <p><b>O motivo nomeia o que faltou</b>, e não diz apenas "incompleto":
     * mandar procurar numa ficha de dezoito perguntas é a versão educada de não
     * dizer nada. A lista inteira vai em {@code perguntasSemResposta}, estruturada,
     * para a tela poder mostrá-la sem recortar esta frase.
     */
    static String faltamResponder(Subtotal subtotal) {
        /*
         * Bloco ausente tem motivo PRÓPRIO, e não "faltam responder: " seguido de
         * nada. Um subtotal nulo sem frase é o traço mudo que este projeto já
         * pagou 26 vezes numa tela só.
         */
        if (subtotal.ausente())
            return "Este modelo não tem as perguntas deste bloco da escala.";

        /*
         * Formulário recém-aberto não precisa que lhe digam quais perguntas
         * faltam: faltam todas, e elas estão logo abaixo. Citar duas e contar o
         * resto ali é ruído com cara de diagnóstico.
         */
        if (subtotal.intocado())
            return subtotal.total() == 1
                    ? "A pergunta deste bloco ainda não foi respondida."
                    : "Nenhuma das " + subtotal.total() + " perguntas deste bloco foi respondida.";

        List<String> nomes = subtotal.semResposta();
        if (nomes.isEmpty()) return null;

        String citadas = nomes.stream().limit(2)
                .map(EscalaNutricional::resumir)
                .collect(Collectors.joining(", "));

        int restantes = nomes.size() - Math.min(2, nomes.size());
        String sufixo = restantes == 0 ? "."
                : restantes == 1 ? " e mais 1." : " e mais " + restantes + ".";

        return (nomes.size() == 1 ? "Falta responder: " : "Faltam responder: ") + citadas + sufixo;
    }

    /**
     * As pendências que vale a pena <b>listar</b>, e não só contar.
     *
     * <p>Vazio enquanto ninguém respondeu nada: numa MNA em branco a lista são as
     * dezoito perguntas, impressas logo acima do formulário que faz exatamente
     * essas dezoito — o questionário duas vezes na mesma tela. Depois que alguém
     * começou, aí sim: <i>"você achou que tinha terminado, faltam estas duas"</i>
     * é a informação que o formulário sozinho não dá, porque ela exige varrer
     * dezoito campos procurando "Não informado".
     */
    static List<String> pendentesVisiveis(Subtotal subtotal) {
        return subtotal.intocado() ? List.of() : subtotal.semResposta();
    }

    /** Enunciado inteiro numa frase vira parágrafo — aqui ele cabe. */
    private static String resumir(String rotulo) {
        String limpo = rotulo.trim();
        return limpo.length() <= 48 ? limpo : limpo.substring(0, 47).trim() + "…";
    }
}
