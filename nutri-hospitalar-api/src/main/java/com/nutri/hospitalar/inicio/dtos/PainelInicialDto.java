package com.nutri.hospitalar.inicio.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A tela inicial: <b>a lista de trabalho do dia</b>, não um resumo gerencial.
 *
 * <p>A pergunta que ela responde é "o que preciso fazer hoje". Paciente de UTI
 * não tem hora marcada — ele está no leito, e a nutricionista faz ronda. Por
 * isso aqui não há agenda: há <b>quem ainda não foi atendido hoje</b>.
 *
 * <p><b>Nenhuma constante clínica nova.</b> O único julgamento desta tela é a
 * adesão abaixo de 70 % fora da primeira semana, que já está documentada em
 * {@code docs/10} com a fonte. O resto — há quantos dias sem avaliação — sai
 * como número e ordem, sem cor e sem limite: não existe no sistema uma regra de
 * "reavaliar a cada N dias", e inventá-la aqui seria a inferência que este
 * projeto recusa.
 */
public record PainelInicialDto(

        LocalDate hoje,

        /** Quem está em acompanhamento e ainda não tem registro de hoje. */
        List<PendenteDoDiaDto> pendentesDeHoje,
        /** Quantos já foram registrados hoje — o denominador da frase da tela. */
        int registradosHoje,
        int totalEmAcompanhamento,

        List<AlertaAdesaoDto> adesaoBaixa,
        List<MudancaDeFaixaDto> mudancasDeFaixa,
        List<SemAvaliacaoDto> haMaisTempoSemAvaliacao,

        NumerosDoMesDto numeros
) {

    /**
     * @param diasSemRegistro dias desde o último registro. Vai como número
     *                        <b>sem julgamento</b> — quem lê decide
     */
    public record PendenteDoDiaDto(UUID pessoaId, String pessoaNome,
                                   LocalDate ultimoDia, int diasSemRegistro,
                                   UUID avaliacaoId) {}

    /**
     * Adesão sustentadamente baixa <b>depois da primeira semana</b>.
     *
     * <p>A ESPEN recomenda oferta abaixo de 70 % nos primeiros dias — e
     * adequação sustentada abaixo de 70 % associa-se a 1,4× mais óbito. O mesmo
     * 60 % é correto no dia 2 e preocupante no dia 10, e é por isso que
     * {@code diasDeTerapia} viaja junto: sem ele o alerta mentiria metade do
     * tempo.
     */
    public record AlertaAdesaoDto(UUID pessoaId, String pessoaNome,
                                  BigDecimal adesaoMedia, int diasConsiderados,
                                  int diasDeTerapia, UUID avaliacaoId) {}

    /**
     * A criança mudou de faixa da OMS entre as duas últimas avaliações.
     *
     * <p>De → para, nos três índices, <b>sem escrever "piorou"</b>: a faixa é o
     * que o sistema gravou; o julgamento é de quem lê.
     */
    public record MudancaDeFaixaDto(UUID pacienteId, String pacienteNome,
                                    LocalDate dataAnterior, LocalDate dataAtual,
                                    int idadeMeses,
                                    String indice, String de, String para,
                                    UUID avaliacaoId) {}

    public record SemAvaliacaoDto(UUID pacienteId, String pacienteNome,
                                  String modulo, LocalDate ultimaAvaliacao,
                                  int diasSemAvaliacao) {}

    /**
     * O volume recente — <b>30 dias corridos</b>, não o mês corrente: tela de
     * trabalho não pode ter um bloco que nasce zerado no dia 1º.
     *
     * @param pacientesUti      pacientes avaliados na janela, não avaliações —
     *                          a consulta devolve uma linha por paciente, e o
     *                          nome do campo diz isso
     * @param diasRegistrados   todos os dias registrados, inclusive os sem
     *                          volume recebido
     */
    public record NumerosDoMesDto(long pacientesUti, long pacientesPediatria,
                                  long diasRegistrados, BigDecimal adesaoMediaUti) {}
}
