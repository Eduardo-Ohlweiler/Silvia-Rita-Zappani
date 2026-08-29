package com.nutri.hospitalar.uti.service;

import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import com.nutri.hospitalar.uti.dtos.DashboardUtiDto;
import com.nutri.hospitalar.uti.dtos.DashboardUtiDto.ContagemDto;
import com.nutri.hospitalar.uti.dtos.DashboardUtiDto.ContagemRotuladaDto;
import com.nutri.hospitalar.uti.dtos.DashboardUtiDto.PacienteRankingDto;
import com.nutri.hospitalar.uti.dtos.DashboardUtiDto.PontoPeriodoDto;
import com.nutri.hospitalar.uti.dtos.PainelAcompanhamentoUtiDto;
import com.nutri.hospitalar.uti.dtos.PainelPacienteUtiDto;
import com.nutri.hospitalar.uti.dtos.PainelPacienteUtiDto.HistoricoFormulaDto;
import com.nutri.hospitalar.uti.dtos.PainelPacienteUtiDto.PontoAvaliacaoDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiResponseDto;
import com.nutri.hospitalar.uti.entity.AvaliacaoUti;
import com.nutri.hospitalar.uti.entity.RegistroDiarioUti;
import com.nutri.hospitalar.uti.mapper.AvaliacaoUtiMapper;
import com.nutri.hospitalar.uti.repository.AvaliacaoUtiRepository;
import com.nutri.hospitalar.uti.repository.RegistroDiarioUtiRepository;
import com.nutri.hospitalar.uti.mapper.RegistroDiarioUtiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Os três painéis da terapia nutricional de UTI: o paciente no tempo, os dias
 * de acompanhamento e a visão gerencial do período.
 *
 * <p><b>Aqui não se calcula nutrição.</b> Este service soma, conta e ordena o
 * que já foi calculado e gravado. Recalcular seria refazer prontuário fechado —
 * a mesma regra do {@link com.nutri.hospitalar.pediatria.service.PediatriaDashboardService}.
 *
 * <p>A única aritmética é a de agregação — média, soma, percentual — e ela vem
 * com duas regras que não são detalhe:
 *
 * <ol>
 *   <li><b>Ausência não é zero.</b> Toda média ignora o registro em que o valor
 *       não existe. Um dia sem lactato não entra na média de lactato; uma
 *       avaliação sem altura não entra na média de IMC. A planilha comete esse
 *       erro ao aplicar {@code MÉDIA} sobre coluna com célula vazia formatada
 *       como zero, e o resultado faz o paciente parecer melhor do que está.</li>
 *   <li><b>Os derivados do dia saem do mapper</b>, não de conta refeita aqui.
 *       {@code percentualRecebido}, {@code caloriasPorQuilo} e
 *       {@code diuresePorQuiloHora} têm um dono só — {@link
 *       RegistroDiarioUtiMapper} — e é por isso que o gráfico e a lista não
 *       conseguem discordar sobre o mesmo dia.</li>
 * </ol>
 *
 * <p>A única chamada ao calculador é a que monta a última avaliação do painel
 * do paciente, e dela se aproveita <b>só o texto</b> que explica um campo
 * vazio — ver {@link AvaliacaoUtiService#paraResposta}. Número nenhum vem de lá.
 *
 * <p>Nada de dado clínico em log, como no resto do módulo.
 */
@Service
@RequiredArgsConstructor
public class UtiDashboardService {

    /** Janela padrão do acompanhamento diário — um mês de beira de leito. */
    private static final int DIAS_PADRAO_ACOMPANHAMENTO = 30;

    /** Teto da série mensal: 24 barras já não cabem em 360 px de largura. */
    private static final int MESES_NA_SERIE = 12;

    private static final DateTimeFormatter MES =
            DateTimeFormatter.ofPattern("MM/yyyy", new Locale("pt", "BR"));

    private static final BigDecimal CEM = new BigDecimal("100");

    /** O rótulo de eutrofia gravado pelo classificador de IMC da OMS. */
    private static final String EUTROFIA = "Eutrofia";

    private final AvaliacaoUtiRepository avaliacaoRepository;
    private final RegistroDiarioUtiRepository registroRepository;
    private final PessoaRepository pessoaRepository;
    private final AvaliacaoUtiService avaliacaoService;
    private final SecurityUtils securityUtils;

    // ─── Painel do paciente ──────────────────────────────────────────────

    /**
     * A trajetória de um paciente: onde está hoje e por onde a terapia passou.
     *
     * @param dias janela em dias; {@code 0} traz desde sempre
     */
    @Transactional(readOnly = true)
    public PainelPacienteUtiDto painelPaciente(UUID pacienteId, int dias, UUID formulaEnteralId) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        Pessoa paciente = pessoaRepository.findByIdAndTenantId(pacienteId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));

        LocalDate desde = desde(dias);

        List<AvaliacaoUti> avaliacoes =
                avaliacaoRepository.findParaPainel(tenantId, pacienteId, formulaEnteralId, desde, null);
        List<RegistroDiarioUti> registros =
                registroRepository.findParaPainel(tenantId, pacienteId, desde, null);

        AvaliacaoUti ultima = avaliacoes.stream()
                .max(Comparator.comparing(AvaliacaoUti::getDataAvaliacao))
                .orElse(null);

        return new PainelPacienteUtiDto(
                paciente.getId(),
                paciente.getNome(),
                // Da pessoa, não da avaliação: paciente sem avaliação nenhuma
                // ainda precisa ser identificado.
                paciente.getSexo() != null ? paciente.getSexo()
                        : (ultima != null ? ultima.getSexo() : null),
                paciente.getDataNascimento(),
                idadeAtual(paciente, ultima),

                avaliacoes.size(),
                primeiraData(avaliacoes, AvaliacaoUti::getDataAvaliacao),
                ultima != null ? ultima.getDataAvaliacao() : null,
                ultima != null ? avaliacaoService.paraResposta(ultima) : null,

                avaliacoes.stream().map(UtiDashboardService::ponto).toList(),
                historicoFormulas(avaliacoes),

                registros.size(),
                primeiraData(registros, RegistroDiarioUti::getData),
                ultimaData(registros, RegistroDiarioUti::getData));
    }

    /**
     * Idade hoje, da data de nascimento. Sem ela, a da última avaliação — é o
     * que se sabe, envelhece mal, e ainda assim é melhor que traço.
     */
    private Integer idadeAtual(Pessoa paciente, AvaliacaoUti ultima) {
        if (paciente.getDataNascimento() != null) {
            return Period.between(paciente.getDataNascimento(), LocalDate.now()).getYears();
        }
        return ultima != null ? ultima.getIdadeAnos() : null;
    }

    private static PontoAvaliacaoDto ponto(AvaliacaoUti a) {
        return new PontoAvaliacaoDto(
                a.getDataAvaliacao(), a.getIdadeAnos(),
                a.getPesoTrabalhoKg(), a.getImc(),
                a.getClassifImcOms(), a.getClassifImcOmsTom(),
                a.getPercPerdaPeso(), a.getClassifPerdaPeso(), a.getClassifPerdaPesoTom(),
                a.getAdequacaoCircBracoPerc(), a.getClassifAdequacaoCb(), a.getClassifAdequacaoCbTom(),
                a.getMetaEnergetica(), a.getMetaProteica(),
                a.getVolumeTotalMl(), a.getCaloriasOfertadas(), a.getProteinaOfertada(),
                a.getCaloriasPorQuilo(), a.getProteinaPorQuilo(),
                a.getPercentualDoVct(), a.getPercentualDaProteina());
    }

    /** Sai do retrato gravado, não do catálogo: a fórmula pode ter sumido. */
    private List<HistoricoFormulaDto> historicoFormulas(List<AvaliacaoUti> avaliacoes) {
        Map<String, List<AvaliacaoUti>> porFormula = avaliacoes.stream()
                .filter(a -> a.getFormulaNome() != null)
                .collect(Collectors.groupingBy(AvaliacaoUti::getFormulaNome,
                        LinkedHashMap::new, Collectors.toList()));

        return porFormula.entrySet().stream()
                .map(e -> new HistoricoFormulaDto(
                        e.getKey(),
                        e.getValue().size(),
                        primeiraData(e.getValue(), AvaliacaoUti::getDataAvaliacao),
                        ultimaData(e.getValue(), AvaliacaoUti::getDataAvaliacao)))
                .sorted(Comparator.comparing(HistoricoFormulaDto::ultimoUso,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    // ─── Painel de acompanhamento ────────────────────────────────────────

    /**
     * Os dias de um paciente: adesão, laboratório, balanço e hemodinâmica.
     *
     * <p>Os dias vêm inteiros, pelo mapper de sempre — ver a nota de
     * {@link PainelAcompanhamentoUtiDto}.
     */
    @Transactional(readOnly = true)
    public PainelAcompanhamentoUtiDto painelAcompanhamento(UUID pessoaId, LocalDate de, LocalDate ate) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        Pessoa pessoa = pessoaRepository.findByIdAndTenantId(pessoaId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));

        LocalDate inicio = de != null ? de
                : LocalDate.now().minusDays(DIAS_PADRAO_ACOMPANHAMENTO - 1L);

        List<RegistroDiarioUti> registros =
                registroRepository.findParaPainel(tenantId, pessoaId, inicio, ate);

        List<RegistroDiarioUtiResponseDto> dias = registros.stream()
                .map(RegistroDiarioUtiMapper::toResponse)
                .toList();

        // A régua é a avaliação vigente no último dia — a mesma que o próprio
        // registro usou para derivar os seus números.
        AvaliacaoUti vigente = registros.isEmpty() ? null
                : registros.get(registros.size() - 1).getAvaliacao();

        return new PainelAcompanhamentoUtiDto(
                pessoa.getId(),
                pessoa.getNome(),
                inicio,
                ate,
                dias.size(),
                dias.stream().filter(d -> d.motivoDerivados() != null).count(),

                media(dias, RegistroDiarioUtiResponseDto::percentualRecebido, 1),
                media(dias, RegistroDiarioUtiResponseDto::caloriasPorQuilo, 1),
                media(dias, RegistroDiarioUtiResponseDto::proteinaPorQuilo, 2),
                soma(dias, RegistroDiarioUtiResponseDto::balancoHidricoMl),
                media(dias, RegistroDiarioUtiResponseDto::diuresePorQuiloHora, 2),
                media(dias, RegistroDiarioUtiResponseDto::mediaIngestaoOral, 1),

                vigente == null ? null : vigente.getDataAvaliacao(),
                vigente == null ? null : vigente.getMetaEnergetica(),
                vigente == null ? null : vigente.getMetaProteica(),
                vigente == null ? null : vigente.getVolumeTotalMl(),

                dias);
    }

    // ─── Painel gerencial ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardUtiDto dashboard(int dias, UUID formulaEnteralId) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        LocalDate desde = desde(dias);

        List<AvaliacaoUti> avaliacoes =
                avaliacaoRepository.findParaPainel(tenantId, null, formulaEnteralId, desde, null);
        List<RegistroDiarioUti> registros =
                registroRepository.findParaPainel(tenantId, null, desde, null);

        List<RegistroDiarioUtiResponseDto> diasDoPeriodo = registros.stream()
                .map(RegistroDiarioUtiMapper::toResponse)
                .toList();

        YearMonth mesAtual = YearMonth.now();

        return new DashboardUtiDto(
                avaliacoes.size(),
                avaliacoes.stream().map(a -> a.getPaciente().getId()).distinct().count(),
                avaliacoes.stream()
                        .filter(a -> YearMonth.from(a.getDataAvaliacao()).equals(mesAtual))
                        .count(),
                registros.size(),

                media(avaliacoes, a -> a.getIdadeAnos() == null
                        ? null : BigDecimal.valueOf(a.getIdadeAnos()), 1),
                media(avaliacoes, AvaliacaoUti::getPesoTrabalhoKg, 2),
                media(avaliacoes, AvaliacaoUti::getImc, 2),
                media(avaliacoes, AvaliacaoUti::getMetaEnergetica, 0),
                media(avaliacoes, AvaliacaoUti::getMetaProteica, 1),
                media(avaliacoes, AvaliacaoUti::getCaloriasPorQuilo, 1),
                media(avaliacoes, AvaliacaoUti::getProteinaPorQuilo, 2),
                media(diasDoPeriodo, RegistroDiarioUtiResponseDto::percentualRecebido, 1),
                percentualEutrofia(avaliacoes),
                avaliacoes.stream().filter(a -> Boolean.TRUE.equals(a.getObeso())).count(),

                serieMensal(avaliacoes, registros),

                distribuicao(avaliacoes, AvaliacaoUti::getClassifImcOms,
                        AvaliacaoUti::getClassifImcOmsTom),
                distribuicao(avaliacoes, AvaliacaoUti::getClassifAdequacaoCb,
                        AvaliacaoUti::getClassifAdequacaoCbTom),
                distribuicao(avaliacoes, AvaliacaoUti::getClassifPerdaPeso,
                        AvaliacaoUti::getClassifPerdaPesoTom),

                contar(avaliacoes, a -> a.getFormulaNome() == null ? "Sem fórmula" : a.getFormulaNome()),
                contar(avaliacoes, a -> a.getFase() == null
                        ? "Não informada" : a.getFase().getDescricao()),
                contar(avaliacoes, a -> a.getTerapiaRenal() == null
                        ? "Não informada" : a.getTerapiaRenal().getDescricao()),
                contar(avaliacoes, a -> a.getModoInfusao() == null
                        ? "Não informado" : a.getModoInfusao().getDescricao()),

                ranking(avaliacoes, registros));
    }

    /**
     * Percentual das avaliações <b>com</b> classificação de IMC que caíram em
     * eutrofia. Quem não tem IMC fica fora do numerador e do denominador — não
     * classificado não é sinônimo de inadequado.
     */
    private BigDecimal percentualEutrofia(List<AvaliacaoUti> avaliacoes) {
        List<String> classificadas = avaliacoes.stream()
                .map(AvaliacaoUti::getClassifImcOms)
                .filter(Objects::nonNull)
                .toList();

        if (classificadas.isEmpty()) return null;

        long eutroficas = classificadas.stream().filter(EUTROFIA::equalsIgnoreCase).count();
        return BigDecimal.valueOf(eutroficas)
                .multiply(CEM)
                .divide(BigDecimal.valueOf(classificadas.size()), 1, RoundingMode.HALF_UP);
    }

    /** Série contínua: mês sem movimento aparece com zero, senão o gráfico mente. */
    private List<PontoPeriodoDto> serieMensal(List<AvaliacaoUti> avaliacoes,
                                              List<RegistroDiarioUti> registros) {
        Map<YearMonth, Long> porMes = avaliacoes.stream()
                .collect(Collectors.groupingBy(a -> YearMonth.from(a.getDataAvaliacao()),
                        Collectors.counting()));
        Map<YearMonth, Long> diasPorMes = registros.stream()
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getData()),
                        Collectors.counting()));

        YearMonth fim = YearMonth.now();
        YearMonth teto = fim.minusMonths(MESES_NA_SERIE - 1L);

        YearMonth inicio = porMes.keySet().stream()
                .min(YearMonth::compareTo)
                .orElse(teto);
        if (inicio.isBefore(teto)) inicio = teto;

        List<PontoPeriodoDto> serie = new ArrayList<>();
        for (YearMonth m = inicio; !m.isAfter(fim); m = m.plusMonths(1)) {
            serie.add(new PontoPeriodoDto(m.atDay(1).format(MES),
                    porMes.getOrDefault(m, 0L),
                    diasPorMes.getOrDefault(m, 0L)));
        }
        return serie;
    }

    /**
     * Distribuição por rótulo gravado.
     *
     * <p>Ordena pelo que é mais frequente, e a fatia "não classificado" vai
     * sempre por último — ela não é uma categoria clínica, é a ausência de uma.
     */
    private List<ContagemRotuladaDto> distribuicao(List<AvaliacaoUti> avaliacoes,
                                                   Function<AvaliacaoUti, String> rotulo,
                                                   Function<AvaliacaoUti, String> tom) {
        Map<String, List<AvaliacaoUti>> porRotulo = avaliacoes.stream()
                .filter(a -> rotulo.apply(a) != null)
                .collect(Collectors.groupingBy(rotulo));

        List<ContagemRotuladaDto> saida = porRotulo.entrySet().stream()
                .map(e -> new ContagemRotuladaDto(
                        e.getKey(),
                        tom.apply(e.getValue().get(0)),
                        e.getValue().size()))
                .sorted(Comparator.comparingLong(ContagemRotuladaDto::quantidade).reversed()
                        .thenComparing(ContagemRotuladaDto::rotulo))
                .collect(Collectors.toCollection(ArrayList::new));

        long sem = avaliacoes.stream().filter(a -> rotulo.apply(a) == null).count();
        if (sem > 0) saida.add(new ContagemRotuladaDto("Não classificado", null, sem));

        return List.copyOf(saida);
    }

    private List<ContagemDto> contar(List<AvaliacaoUti> avaliacoes,
                                     Function<AvaliacaoUti, String> chave) {
        return avaliacoes.stream()
                .collect(Collectors.groupingBy(chave, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ContagemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ContagemDto::quantidade).reversed()
                        .thenComparing(ContagemDto::rotulo))
                .toList();
    }

    /** Os dez com mais avaliações, com os dias de acompanhamento ao lado. */
    private List<PacienteRankingDto> ranking(List<AvaliacaoUti> avaliacoes,
                                             List<RegistroDiarioUti> registros) {
        Map<UUID, Long> diasPorPaciente = registros.stream()
                .collect(Collectors.groupingBy(r -> r.getPessoa().getId(), Collectors.counting()));

        return avaliacoes.stream()
                .collect(Collectors.groupingBy(AvaliacaoUti::getPaciente, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new PacienteRankingDto(
                        e.getKey().getId(), e.getKey().getNome(), e.getValue(),
                        diasPorPaciente.getOrDefault(e.getKey().getId(), 0L)))
                .sorted(Comparator.comparingLong(PacienteRankingDto::avaliacoes).reversed()
                        .thenComparing(PacienteRankingDto::pacienteNome))
                .limit(10)
                .toList();
    }

    // ─── Aritmética de agregação ─────────────────────────────────────────

    /**
     * {@code dias <= 0} significa "desde sempre" — e aí não há corte inferior.
     *
     * <p>O padrão de cada endpoint fica no {@code defaultValue} do controller,
     * como na pediatria: 0 no painel de um paciente (a trajetória inteira é o
     * ponto) e 365 no gerencial.
     */
    private LocalDate desde(int dias) {
        if (dias <= 0) return null;
        return LocalDate.now().minusDays(dias);
    }

    private <T> BigDecimal media(List<T> itens, Function<T, BigDecimal> valor, int escala) {
        List<BigDecimal> valores = itens.stream()
                .map(valor)
                .filter(Objects::nonNull)
                .toList();

        if (valores.isEmpty()) return null;

        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), escala, RoundingMode.HALF_UP);
    }

    /**
     * Soma que devolve {@code null} quando não há nenhuma parcela.
     *
     * <p>Zero e "nenhum dia informado" são coisas diferentes: um balanço hídrico
     * acumulado de zero é equilíbrio, e a ausência dele é falta de dado.
     */
    private <T> BigDecimal soma(List<T> itens, Function<T, BigDecimal> valor) {
        List<BigDecimal> valores = itens.stream()
                .map(valor)
                .filter(Objects::nonNull)
                .toList();

        return valores.isEmpty() ? null
                : valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> LocalDate primeiraData(List<T> itens, Function<T, LocalDate> data) {
        return itens.stream().map(data).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
    }

    private <T> LocalDate ultimaData(List<T> itens, Function<T, LocalDate> data) {
        return itens.stream().map(data).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
    }
}
