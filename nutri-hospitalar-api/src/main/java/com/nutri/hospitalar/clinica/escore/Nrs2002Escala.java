package com.nutri.hospitalar.clinica.escore;

import com.nutri.hospitalar.clinica.dtos.ClassificacaoEscoreDto;
import com.nutri.hospitalar.clinica.dtos.EscoreDto;
import com.nutri.hospitalar.clinica.dtos.GrupoEscoreDto;
import com.nutri.hospitalar.clinica.escore.SomaPorGrupo.Subtotal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NRS-2002 — Nutritional Risk Screening. Especificação em {@code docs/13 §3}.
 *
 * <p>Fonte: Kondrup J, Rasmussen HH, Hamberg O, Stanga Z; ad hoc ESPEN Working
 * Group. <i>Nutritional risk screening (NRS 2002): a new method based on an
 * analysis of controlled clinical trials.</i> Clin Nutr 2003;22(3):321-336.
 *
 * <p>Duas coisas desta classe <b>não estavam</b> na descrição recebida da cliente
 * e entram por serem da publicação original — decisão registrada em docs/13 §3.7:
 * o <b>ponto por idade</b> (≥ 70 anos) e o <b>corte ≥ 3</b>. Sem o primeiro, todo
 * paciente idoso pontua um a menos; e como o corte é 3, um idoso com etapa 2 = 2 e
 * etapa 3 = 0 sairia com 2 (<i>reavaliar semanalmente</i>) onde o instrumento diz
 * 3 (<i>em risco, iniciar terapia</i>). É a diferença que decide a conduta.
 */
public final class Nrs2002Escala implements EscalaNutricional {

    static final String PRE_TRIAGEM = "PRE_TRIAGEM";
    static final String ESTADO = "ESTADO_NUTRICIONAL";
    static final String GRAVIDADE = "GRAVIDADE_DOENCA";

    private static final BigDecimal MAX_ETAPA = new BigDecimal("3");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("7");

    /** docs/13 §3.5 e §3.6. */
    private static final int IDADE_QUE_PONTUA = 70;
    private static final BigDecimal PONTO_DA_IDADE = new BigDecimal("1.0");
    private static final BigDecimal CORTE_DE_RISCO = new BigDecimal("3");

    @Override public String codigo() { return "NRS_2002"; }

    @Override public String nome() { return "NRS-2002 — Triagem de risco nutricional"; }

    @Override public String referencia() {
        return "Kondrup et al., ESPEN. Clin Nutr 2003;22(3):321-336";
    }

    /** A pré-triagem não pontua: o máximo dela é zero, e isso é verificável. */
    @Override
    public Map<String, BigDecimal> maximoPorGrupo() {
        Map<String, BigDecimal> maximos = new LinkedHashMap<>();
        maximos.put(PRE_TRIAGEM, BigDecimal.ZERO);
        maximos.put(ESTADO, MAX_ETAPA);
        maximos.put(GRAVIDADE, MAX_ETAPA);
        return maximos;
    }

    @Override
    public EscoreDto avaliar(List<ItemRespondido> itens, Integer idadeAnos) {
        Map<String, Subtotal> subtotais = SomaPorGrupo.de(itens);
        Subtotal pre = subtotais.getOrDefault(PRE_TRIAGEM, SomaPorGrupo.vazio());
        Subtotal estado = subtotais.getOrDefault(ESTADO, SomaPorGrupo.vazio());
        Subtotal gravidade = subtotais.getOrDefault(GRAVIDADE, SomaPorGrupo.vazio());

        boolean portaAberta = algumCriterioPresente(itens);

        List<GrupoEscoreDto> grupos = List.of(
                grupoPreTriagem(pre, portaAberta),
                etapa(ESTADO, "Estado nutricional", estado),
                etapa(GRAVIDADE, "Gravidade da doença", gravidade));

        return montarTotal(pre, estado, gravidade, portaAberta, idadeAnos, grupos);
    }

    /**
     * A pré-triagem é uma <b>porta</b>, não uma soma.
     *
     * <p>Quatro "não" encerram o instrumento com a conduta <i>repetir a triagem
     * semanalmente</i> — o que é <b>resultado da escala</b>, e não falta de dado.
     * A diferença aparece na tela: porta fechada produz conclusão, não uma queixa
     * de campo em branco. Por isso este grupo não tem subtotal nem máximo: ele
     * não pontua, e um "0 / 0" ao lado dele seria número onde não há conta.
     */
    private GrupoEscoreDto grupoPreTriagem(Subtotal pre, boolean portaAberta) {
        if (!pre.completo()) {
            return new GrupoEscoreDto(PRE_TRIAGEM, "Pré-triagem", null, null, null,
                    "Responda as quatro perguntas da pré-triagem.",
                    EscalaNutricional.pendentesVisiveis(pre));
        }
        ClassificacaoEscoreDto conclusao = portaAberta
                ? ClassificacaoEscoreDto.neutra("Critério presente — aplicar as etapas 2 e 3")
                : ClassificacaoEscoreDto.adequada("Nenhum critério presente");

        return new GrupoEscoreDto(PRE_TRIAGEM, "Pré-triagem", null, null, conclusao,
                null, List.of());
    }

    private GrupoEscoreDto etapa(String grupo, String rotulo, Subtotal subtotal) {
        if (subtotal.completo()) {
            return new GrupoEscoreDto(grupo, rotulo, subtotal.soma(), MAX_ETAPA,
                    null, null, List.of());
        }
        return new GrupoEscoreDto(grupo, rotulo, null, MAX_ETAPA, null,
                EscalaNutricional.faltamResponder(subtotal),
                EscalaNutricional.pendentesVisiveis(subtotal));
    }

    /**
     * "Qualquer uma sim" abre a porta.
     *
     * <p>Lê o valor cru do sim/não, e não o ponto — a pré-triagem não pontua. O
     * {@code CHECKBOX} deste sistema tem três estados, e só {@code "true"} conta:
     * não respondida é ausência, e ausência não é "não". Essa distinção é o que
     * impede a porta de se fechar sozinha num formulário em branco.
     */
    private boolean algumCriterioPresente(List<ItemRespondido> itens) {
        return itens.stream()
                .filter(i -> PRE_TRIAGEM.equals(i.grupo()))
                .anyMatch(i -> "true".equals(i.valor()));
    }

    /**
     * Os cinco caminhos, com {@code else} final.
     *
     * <p>Um deles — a porta fechada — <b>não é ausência</b>: é o instrumento
     * concluindo sem número. Por isso ele preenche {@code classificacao} e
     * {@code conclusao} em vez de {@code motivoAusencia}: um traço sozinho na
     * tela faria o profissional achar que o sistema falhou, quando a escala
     * respondeu.
     */
    private EscoreDto montarTotal(Subtotal pre, Subtotal estado, Subtotal gravidade,
                                  boolean portaAberta, Integer idadeAnos,
                                  List<GrupoEscoreDto> grupos) {

        BigDecimal total = null;
        BigDecimal ajusteIdade = null;
        String ajusteDescricao = null;
        ClassificacaoEscoreDto classificacao = null;
        String conclusao = null;
        String motivo = null;

        if (!pre.completo()) {
            motivo = "Responda as quatro perguntas da pré-triagem.";

        } else if (!portaAberta) {
            classificacao = ClassificacaoEscoreDto.adequada("Sem risco na triagem inicial");
            conclusao = "Nenhum critério da pré-triagem — repetir a triagem semanalmente.";

        } else if (!estado.completo() || !gravidade.completo()) {
            motivo = etapasQueFaltam(estado, gravidade);

        } else if (idadeAnos == null) {
            motivo = "Paciente sem data de nascimento: o ponto por idade "
                    + "(70 anos ou mais) não pode ser decidido.";

        } else {
            boolean idoso = idadeAnos >= IDADE_QUE_PONTUA;
            ajusteIdade = idoso ? PONTO_DA_IDADE : BigDecimal.ZERO.setScale(1);
            ajusteDescricao = (idoso ? "Idade de 70 anos ou mais" : "Idade abaixo de 70 anos")
                    + " — " + idadeAnos + " anos na data de preenchimento";

            total = estado.soma().add(gravidade.soma()).add(ajusteIdade);

            if (total.compareTo(CORTE_DE_RISCO) >= 0) {
                classificacao = ClassificacaoEscoreDto.atencao("Em risco nutricional");
                conclusao = "Iniciar plano de terapia nutricional.";
            } else {
                classificacao = ClassificacaoEscoreDto.adequada("Sem risco nutricional no momento");
                conclusao = "Reavaliar semanalmente.";
            }
        }

        return new EscoreDto(codigo(), nome(), referencia(), grupos,
                ajusteIdade, ajusteDescricao,
                total, MAX_TOTAL, classificacao, conclusao, motivo);
    }

    /** Motivo é do tamanho da coisa que faltou — nomeia a etapa, não as duas. */
    private String etapasQueFaltam(Subtotal estado, Subtotal gravidade) {
        List<String> faltando = new ArrayList<>();
        if (!estado.completo()) faltando.add("o estado nutricional");
        if (!gravidade.completo()) faltando.add("a gravidade da doença");
        return "O escore total exige " + String.join(" e ", faltando) + ".";
    }
}
