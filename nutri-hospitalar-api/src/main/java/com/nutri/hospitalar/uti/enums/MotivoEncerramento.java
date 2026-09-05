package com.nutri.hospitalar.uti.enums;

/**
 * Por que o acompanhamento nutricional terminou.
 *
 * <p><b>Existe porque a lista de trabalho precisa de uma saída.</b> Sem
 * encerramento, o paciente que recebeu alta nunca deixa a tela inicial: ela
 * cobraria todo dia por alguém que não está mais no leito, e em uma semana
 * estaria cheia de fantasmas — o que é pior do que não ter lista, porque quem
 * usa aprende a ignorá-la.
 *
 * <p><b>{@link #OBITO} não é eufemismo nem omissão.</b> Em terapia intensiva o
 * desfecho nem sempre é alta, e um serviço de nutrição precisa responder no fim
 * do mês quantos pacientes acompanhou e como terminaram. Registrar "alta" onde
 * houve óbito falsearia a própria estatística do serviço.
 *
 * <p>Não confundir com <b>alta hospitalar do paciente</b>: o que se encerra aqui
 * é o acompanhamento nutricional. {@link #SUSPENSAO_TERAPIA} é o caso em que o
 * paciente segue internado e a terapia nutricional não é mais indicada.
 */
public enum MotivoEncerramento {

    ALTA_HOSPITALAR("Alta hospitalar"),
    OBITO("Óbito"),
    TRANSFERENCIA("Transferência"),
    SUSPENSAO_TERAPIA("Suspensão da terapia nutricional"),
    OUTRO("Outro");

    private final String descricao;

    MotivoEncerramento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
