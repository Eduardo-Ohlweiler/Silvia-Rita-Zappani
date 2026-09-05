package com.nutri.hospitalar.inicio.service;

import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.inicio.dtos.PainelInicialDto;
import com.nutri.hospitalar.pediatria.repository.AvaliacaoPediatricaRepository;
import com.nutri.hospitalar.uti.repository.AvaliacaoUtiRepository;
import com.nutri.hospitalar.uti.repository.RegistroDiarioUtiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A tela inicial — <b>a lista de trabalho do dia</b>.
 *
 * <p>Só leitura e só agregação, como os outros painéis: nada aqui recalcula
 * nutrição. A diferença é a pergunta. Os painéis "em números" respondem "como
 * foi o período"; esta tela responde <b>"o que preciso fazer hoje"</b>.
 *
 * <p><b>Nenhuma constante clínica nova.</b> O único julgamento é o alerta de
 * adesão, e a régua já é do projeto ({@code docs/10}, ESPEN). O resto sai como
 * número e ordem, sem cor e sem limite — não existe no sistema regra de
 * "reavaliar a cada N dias", e inventá-la aqui seria a inferência que este
 * projeto recusa.
 *
 * <p><b>Nada de dado clínico em log</b> (regra 5): nem nome, nem adesão, nem
 * desfecho.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PainelInicialService {

    /**
     * A janela que define "em acompanhamento" — <b>convenção nossa, não regra
     * clínica</b>.
     *
     * <p>É rede, não critério: existe para o paciente que ninguém encerrou não
     * ficar cobrando para sempre. Quem tem alta registrada sai na hora, pelo
     * encerramento; esta janela pega só o esquecido.
     */
    private static final int DIAS_DA_JANELA = 7;

    /**
     * O piso de adesão da ESPEN, e a semana que o torna aplicável.
     *
     * <p>Oferta abaixo de 70 % nos primeiros dias é <b>conduta</b> — a própria
     * planilha progride 25 · 50 · 75 · 100 %. Depois da primeira semana, a
     * adequação sustentada abaixo de 70 % associa-se a 1,4× mais óbito. O mesmo
     * 60 % é correto no dia 2 e preocupante no dia 10, e é por isso que o
     * alerta tem as duas metades. Ver {@code docs/10} e o {@code CLAUDE.md}.
     */
    private static final BigDecimal ADESAO_MINIMA = new BigDecimal("70");
    private static final int DIAS_DA_PRIMEIRA_SEMANA = 7;

    /** Quantas linhas cabem numa tela de entrada sem virar relatório. */
    private static final int TETO_DA_LISTA = 10;

    /** A janela do bloco de volume recente. Corridos, para não nascer zerado no dia 1º. */
    private static final int DIAS_DA_JANELA_LONGA = 30;

    private final RegistroDiarioUtiRepository registroDiarioUtiRepository;
    private final AvaliacaoUtiRepository avaliacaoUtiRepository;
    private final AvaliacaoPediatricaRepository avaliacaoPediatricaRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public PainelInicialDto montar() {
        UUID tenantId = securityUtils.getTenantIdLogado();
        LocalDate hoje = LocalDate.now();
        LocalDate desde = hoje.minusDays(DIAS_DA_JANELA);

        var emAcompanhamento =
                registroDiarioUtiRepository.findEmAcompanhamento(tenantId, desde, hoje);
        Map<UUID, UUID> registroDeHojePorPessoa = new HashMap<>();
        registroDiarioUtiRepository.findRegistrosEm(tenantId, hoje)
                .forEach(r -> registroDeHojePorPessoa.put(r.getPessoaId(), r.getRegistroId()));

        // A ronda INTEIRA, com o estado de cada um — não só o que falta.
        // Pendente primeiro, porque é o que cobra ação; dentro de cada grupo, o
        // mais antigo antes, que é a ordem em que a consulta já vem.
        List<PainelInicialDto.LinhaDaRondaDto> ronda = emAcompanhamento.stream()
                .map(p -> new PainelInicialDto.LinhaDaRondaDto(
                        p.getPessoaId(), p.getPessoaNome(),
                        p.getUltimoDia(), p.getDiasSemRegistro(),
                        registroDeHojePorPessoa.containsKey(p.getPessoaId()),
                        registroDeHojePorPessoa.get(p.getPessoaId()),
                        p.getAvaliacaoId()))
                .sorted(Comparator.comparing(PainelInicialDto.LinhaDaRondaDto::registradoHoje))
                .toList();

        return new PainelInicialDto(
                hoje,
                ronda,
                (int) emAcompanhamento.stream()
                        .filter(p -> registroDeHojePorPessoa.containsKey(p.getPessoaId())).count(),
                emAcompanhamento.size(),
                adesaoBaixa(tenantId, desde, hoje, emAcompanhamento),
                mudancasDeFaixa(tenantId, hoje),
                haMaisTempoSemAvaliacao(tenantId, hoje),
                numeros(tenantId, hoje));
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * O alerta só existe <b>depois da primeira semana</b> de terapia.
     *
     * <p>Sem essa metade da regra, a tela acenderia para todo paciente em
     * progressão — que é exatamente o que a ESPEN recomenda fazer — e a
     * nutricionista aprenderia a ignorar o alerta em três dias.
     */
    private List<PainelInicialDto.AlertaAdesaoDto> adesaoBaixa(
            UUID tenantId, LocalDate desde, LocalDate hoje,
            List<RegistroDiarioUtiRepository.PacienteEmAcompanhamento> emAcompanhamento) {

        Map<UUID, RegistroDiarioUtiRepository.AdesaoDoPaciente> porPessoa = new HashMap<>();
        registroDiarioUtiRepository.findAdesaoMediaNaJanela(tenantId, desde, hoje)
                .forEach(a -> porPessoa.put(a.getPessoaId(), a));

        List<PainelInicialDto.AlertaAdesaoDto> fora = new ArrayList<>();

        for (var p : emAcompanhamento) {
            var adesao = porPessoa.get(p.getPessoaId());
            if (adesao == null || adesao.getAdesaoMedia() == null) continue;

            Integer diasDeTerapia = p.getDiasDeTerapia();
            if (diasDeTerapia == null || diasDeTerapia <= DIAS_DA_PRIMEIRA_SEMANA) continue;

            BigDecimal media = adesao.getAdesaoMedia().setScale(1, RoundingMode.HALF_UP);
            if (media.compareTo(ADESAO_MINIMA) >= 0) continue;

            fora.add(new PainelInicialDto.AlertaAdesaoDto(
                    p.getPessoaId(), p.getPessoaNome(), media,
                    adesao.getDias(), diasDeTerapia, p.getAvaliacaoId()));
        }

        fora.sort(Comparator.comparing(PainelInicialDto.AlertaAdesaoDto::adesaoMedia));
        return fora.stream().limit(TETO_DA_LISTA).toList();
    }

    /**
     * Quem mudou de faixa entre as duas últimas avaliações.
     *
     * <p>Compara dois rótulos já gravados — zero régua nova. E <b>não escreve
     * "piorou"</b>: mostra de → para, e quem lê julga. Uma criança que sobe de
     * "Baixo peso" para "Peso adequado" também interessa, e chamá-la de piora
     * seria mentira.
     */
    private List<PainelInicialDto.MudancaDeFaixaDto> mudancasDeFaixa(UUID tenantId, LocalDate hoje) {
        Map<UUID, List<AvaliacaoPediatricaRepository.DuasUltimasAvaliacoes>> porCrianca =
                new java.util.LinkedHashMap<>();

        avaliacaoPediatricaRepository.findDuasUltimasPorPaciente(tenantId, hoje)
                .forEach(a -> porCrianca.computeIfAbsent(a.getPacienteId(), k -> new ArrayList<>()).add(a));

        List<PainelInicialDto.MudancaDeFaixaDto> mudancas = new ArrayList<>();

        for (var par : porCrianca.values()) {
            // Uma avaliação só: não há de onde comparar, e inventar um "antes"
            // seria pior do que ficar calado.
            if (par.size() < 2) continue;

            var atual = par.get(0);
            var anterior = par.get(1);

            acrescentarSeMudou(mudancas, atual, anterior,
                    "Peso para a idade", anterior.getPesoIdade(), atual.getPesoIdade());
            acrescentarSeMudou(mudancas, atual, anterior,
                    "Estatura para a idade", anterior.getEstaturaIdade(), atual.getEstaturaIdade());
            acrescentarSeMudou(mudancas, atual, anterior,
                    "IMC para a idade", anterior.getImcIdade(), atual.getImcIdade());
        }

        mudancas.sort(Comparator.comparing(PainelInicialDto.MudancaDeFaixaDto::dataAtual).reversed());
        return mudancas.stream().limit(TETO_DA_LISTA).toList();
    }

    private void acrescentarSeMudou(List<PainelInicialDto.MudancaDeFaixaDto> destino,
                                    AvaliacaoPediatricaRepository.DuasUltimasAvaliacoes atual,
                                    AvaliacaoPediatricaRepository.DuasUltimasAvaliacoes anterior,
                                    String indice, String de, String para) {
        // Sem classificação de um dos lados não houve mudança observável — a
        // criança pode ter ficado sem estatura naquele dia.
        if (de == null || para == null || de.equals(para)) return;

        destino.add(new PainelInicialDto.MudancaDeFaixaDto(
                atual.getPacienteId(), atual.getPacienteNome(),
                anterior.getDataAvaliacao(), atual.getDataAvaliacao(),
                atual.getIdadeMeses() == null ? 0 : atual.getIdadeMeses(),
                indice, rotulo(indice, de), rotulo(indice, para), atual.getAvaliacaoId()));
    }

    /**
     * A faixa por extenso, <b>e o rótulo depende do índice</b>.
     *
     * <p>"Baixo peso", "Baixa estatura" e "Magreza" são a MESMA faixa em índices
     * diferentes — o {@code FaixaOms} do backend diz isso no próprio javadoc.
     * Um mapa único faria a tela chamar de "Baixa" uma criança com sobrepeso.
     */
    private String rotulo(String indice, String faixa) {
        return switch (indice) {
            case "Peso para a idade" -> switch (faixa) {
                case "BAIXA" -> "Baixo peso";
                case "ADEQUADA" -> "Peso adequado";
                default -> "Acima do peso";
            };
            case "Estatura para a idade" -> switch (faixa) {
                case "BAIXA" -> "Baixa estatura";
                case "ADEQUADA" -> "Adequada";
                default -> "Estatura alta";
            };
            default -> switch (faixa) {
                case "BAIXA" -> "Magreza";
                case "ADEQUADA" -> "IMC adequado";
                default -> "Sobrepeso";
            };
        };
    }

    /**
     * Há quanto tempo cada paciente não é avaliado — <b>sem julgamento</b>.
     *
     * <p>Não existe no sistema uma regra de "reavaliar a cada N dias". A lista
     * sai ordenada do mais antigo e sem cor: o número e a ordem já respondem, e
     * cravar um limite aqui seria inventar constante clínica numa tela.
     */
    private List<PainelInicialDto.SemAvaliacaoDto> haMaisTempoSemAvaliacao(
            UUID tenantId, LocalDate hoje) {

        List<PainelInicialDto.SemAvaliacaoDto> todos = new ArrayList<>();

        // O paciente com acompanhamento encerrado sai daqui também: senão ele
        // acumularia "dias sem avaliação" para sempre, que é o mesmo fantasma
        // que o encerramento existe para tirar da ronda.
        avaliacaoUtiRepository.findUltimaPorPaciente(tenantId, hoje).stream()
                .filter(u -> !Boolean.TRUE.equals(u.getEncerrado()))
                .forEach(u -> todos.add(new PainelInicialDto.SemAvaliacaoDto(
                        u.getPacienteId(), u.getPacienteNome(), "Terapia nutricional",
                        u.getUltima(), (int) java.time.temporal.ChronoUnit.DAYS.between(u.getUltima(), hoje))));

        avaliacaoPediatricaRepository.findUltimaPorPaciente(tenantId, hoje).forEach(u ->
                todos.add(new PainelInicialDto.SemAvaliacaoDto(
                        u.getPacienteId(), u.getPacienteNome(), "Pediatria",
                        u.getUltima(), (int) java.time.temporal.ChronoUnit.DAYS.between(u.getUltima(), hoje))));

        todos.sort(Comparator.comparing(PainelInicialDto.SemAvaliacaoDto::diasSemAvaliacao).reversed());
        return todos.stream().limit(TETO_DA_LISTA).toList();
    }

    /**
     * O volume recente. <b>Trinta dias corridos, e não o mês corrente.</b>
     *
     * <p>Esta é tela de trabalho, não relatório: no dia 1º de cada mês o bloco
     * "este mês" nasceria zerado e pareceria quebrado, um dia em cada trinta.
     * O recorte de fechamento existe nas telas <i>em números</i>, com filtro de
     * período, que é onde ele cabe.
     *
     * <p><b>O rótulo diz o que a conta faz.</b> {@code findUltimaPorPaciente}
     * devolve uma linha por paciente, então isto conta <b>pacientes avaliados</b>
     * — não avaliações. Chamar de "avaliações" seria um número plausível e
     * errado, do tipo que ninguém confere.
     */
    private PainelInicialDto.NumerosDoMesDto numeros(UUID tenantId, LocalDate hoje) {
        LocalDate desde = hoje.minusDays(DIAS_DA_JANELA_LONGA);

        long uti = avaliacaoUtiRepository.findUltimaPorPaciente(tenantId, hoje).stream()
                .filter(u -> !u.getUltima().isBefore(desde)).count();
        long ped = avaliacaoPediatricaRepository.findUltimaPorPaciente(tenantId, hoje).stream()
                .filter(u -> !u.getUltima().isBefore(desde)).count();

        var adesoes = registroDiarioUtiRepository.findAdesaoMediaNaJanela(tenantId, desde, hoje);

        long dias = registroDiarioUtiRepository.contarDiasNaJanela(tenantId, desde, hoje);

        BigDecimal adesaoMedia = adesoes.stream()
                .map(RegistroDiarioUtiRepository.AdesaoDoPaciente::getAdesaoMedia)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PainelInicialDto.NumerosDoMesDto(uti, ped, dias,
                adesoes.isEmpty() ? null
                        : adesaoMedia.divide(BigDecimal.valueOf(adesoes.size()), 1, RoundingMode.HALF_UP));
    }
}
