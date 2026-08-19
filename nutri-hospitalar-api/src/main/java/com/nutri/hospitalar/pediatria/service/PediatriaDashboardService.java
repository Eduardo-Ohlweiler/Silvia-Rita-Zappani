package com.nutri.hospitalar.pediatria.service;

import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pediatria.dtos.CurvaOmsPontoDto;
import com.nutri.hospitalar.pediatria.dtos.DashboardGeralDto;
import com.nutri.hospitalar.pediatria.dtos.DashboardGeralDto.ContagemDto;
import com.nutri.hospitalar.pediatria.dtos.DashboardGeralDto.ContagemFaixaDto;
import com.nutri.hospitalar.pediatria.dtos.DashboardGeralDto.PacienteRankingDto;
import com.nutri.hospitalar.pediatria.dtos.DashboardGeralDto.PontoPeriodoDto;
import com.nutri.hospitalar.pediatria.dtos.PainelPacienteDto;
import com.nutri.hospitalar.pediatria.dtos.PainelPacienteDto.HistoricoFormulaDto;
import com.nutri.hospitalar.pediatria.dtos.PainelPacienteDto.PontoEvolutivoDto;
import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
import com.nutri.hospitalar.pediatria.enums.FaixaOms;
import com.nutri.hospitalar.pediatria.enums.IndiceOms;
import com.nutri.hospitalar.pediatria.mapper.AvaliacaoPediatricaMapper;
import com.nutri.hospitalar.pediatria.repository.AvaliacaoPediatricaRepository;
import com.nutri.hospitalar.pediatria.repository.PercentilOmsRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Os dois painéis da pediatria: o acompanhamento de uma criança e a visão
 * gerencial do período.
 *
 * <p><b>Aqui não se calcula nutrição.</b> Tudo o que este service faz é somar,
 * contar e ordenar o que já foi calculado e gravado. Recalcular seria refazer
 * um prontuário fechado — ver {@link AvaliacaoPediatricaService}.
 *
 * <p>Médias ignoram avaliação sem o valor: a média de IMC não conta quem não
 * tinha estatura. Contar como zero faria a clínica parecer pior do que é.
 */
@Service
@RequiredArgsConstructor
public class PediatriaDashboardService {

    /** Janela padrão do painel gerencial. `dias = 0` significa "desde sempre". */
    private static final int DIAS_PADRAO = 365;

    /** Meses exibidos na série temporal — um ano cabe na tela, dois não. */
    private static final int MESES_NA_SERIE = 12;

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yy");

    /** Faixas etárias em meses, nos cortes que a pediatria usa. */
    private static final int[] FAIXA_LIMITES = {6, 12, 24, 36, 60};
    private static final String[] FAIXA_ROTULOS = {
            "0 a 6 m", "6 a 12 m", "12 a 24 m", "24 a 36 m", "36 a 60 m", "acima de 60 m"};

    private final AvaliacaoPediatricaRepository avaliacaoRepository;
    private final PercentilOmsRepository percentilOmsRepository;
    private final PessoaRepository pessoaRepository;
    private final SecurityUtils securityUtils;

    // ─── Curva de referência ─────────────────────────────────────────────

    /**
     * A janela da curva da OMS que o gráfico vai desenhar.
     *
     * <p>Dado de referência, sem tenant: é a mesma curva para todo cliente.
     */
    @Transactional(readOnly = true)
    public List<CurvaOmsPontoDto> curva(Sexo sexo, Integer idadeMin, Integer idadeMax) {
        int de = idadeMin == null ? 0 : Math.max(0, idadeMin);
        int ate = idadeMax == null ? 60 : Math.min(60, idadeMax);

        return percentilOmsRepository
                .findBySexoAndIdadeMesesBetweenOrderByIdadeMesesAsc(sexo, de, ate)
                .stream()
                .map(p -> new CurvaOmsPontoDto(
                        p.getIdadeMeses(),
                        p.getPesoP3(), p.getPesoP15(), p.getPesoP50(), p.getPesoP85(), p.getPesoP97(),
                        p.getEstaturaP3(), p.getEstaturaP15(), p.getEstaturaP50(),
                        p.getEstaturaP85(), p.getEstaturaP97(),
                        p.getImcP3(), p.getImcP15(), p.getImcP50(), p.getImcP85(), p.getImcP97()))
                .toList();
    }

    // ─── Painel do paciente ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PainelPacienteDto painelPaciente(UUID pacienteId, int dias,
                                            UUID formulaLacteaId,
                                            Integer mesesMin, Integer mesesMax) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        Pessoa paciente = pessoaRepository.findByIdAndTenantId(pacienteId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));

        List<AvaliacaoPediatrica> avaliacoes = avaliacaoRepository.findParaPainel(
                tenantId, pacienteId, formulaLacteaId, desde(dias), mesesMin, mesesMax, null);

        AvaliacaoPediatrica ultima = avaliacoes.stream()
                .max(Comparator.comparing(AvaliacaoPediatrica::getDataAvaliacao))
                .orElse(null);

        return new PainelPacienteDto(
                paciente.getId(),
                paciente.getNome(),
                // O sexo é da pessoa: uma criança sem avaliação nenhuma ainda
                // precisa da curva certa para comparar.
                paciente.getSexo() != null ? paciente.getSexo()
                        : (ultima != null ? ultima.getSexo() : null),
                paciente.getDataNascimento(),
                idadeAtual(paciente, ultima),
                avaliacoes.size(),
                avaliacoes.stream().map(AvaliacaoPediatrica::getDataAvaliacao)
                        .min(LocalDate::compareTo).orElse(null),
                ultima != null ? ultima.getDataAvaliacao() : null,
                ultima != null ? AvaliacaoPediatricaMapper.toResponse(ultima) : null,
                evolucao(avaliacoes),
                historicoFormulas(avaliacoes));
    }

    /**
     * Idade hoje, da data de nascimento. Sem ela, a da última avaliação — é o
     * que se sabe, e some com o tempo, mas melhor que traço.
     */
    private Integer idadeAtual(Pessoa paciente, AvaliacaoPediatrica ultima) {
        if (paciente.getDataNascimento() != null) {
            return (int) Period.between(paciente.getDataNascimento(), LocalDate.now()).toTotalMonths();
        }
        return ultima != null ? ultima.getIdadeMeses() : null;
    }

    /** Ordenada por idade: é o eixo X das curvas. */
    private List<PontoEvolutivoDto> evolucao(List<AvaliacaoPediatrica> avaliacoes) {
        return avaliacoes.stream()
                .sorted(Comparator.comparing(AvaliacaoPediatrica::getIdadeMeses)
                        .thenComparing(AvaliacaoPediatrica::getDataAvaliacao))
                .map(a -> new PontoEvolutivoDto(
                        a.getDataAvaliacao(), a.getIdadeMeses(),
                        a.getPeso(), a.getEstatura(), a.getImc(),
                        a.getClassifPesoIdade(), a.getClassifEstaturaIdade(), a.getClassifImcIdade(),
                        a.getVet(), a.getProteinaNecessidade(),
                        a.getCaloriasTotais(), a.getProteinaTotal(),
                        a.getPercCalorico(), a.getPercProteico()))
                .toList();
    }

    /** Sai do retrato gravado, não do catálogo: a fórmula pode ter sumido. */
    private List<HistoricoFormulaDto> historicoFormulas(List<AvaliacaoPediatrica> avaliacoes) {
        Map<String, List<AvaliacaoPediatrica>> porFormula = avaliacoes.stream()
                .filter(a -> a.getFormulaNome() != null)
                .collect(Collectors.groupingBy(AvaliacaoPediatrica::getFormulaNome,
                        LinkedHashMap::new, Collectors.toList()));

        return porFormula.entrySet().stream()
                .map(e -> new HistoricoFormulaDto(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().map(AvaliacaoPediatrica::getDataAvaliacao)
                                .min(LocalDate::compareTo).orElse(null),
                        e.getValue().stream().map(AvaliacaoPediatrica::getDataAvaliacao)
                                .max(LocalDate::compareTo).orElse(null)))
                .sorted(Comparator.comparing(HistoricoFormulaDto::ultimoUso,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    // ─── Painel gerencial ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardGeralDto dashboardGeral(int dias, UUID formulaLacteaId,
                                            Integer mesesMin, Integer mesesMax, Sexo sexo) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        List<AvaliacaoPediatrica> avaliacoes = avaliacaoRepository.findParaPainel(
                tenantId, null, formulaLacteaId, desde(dias), mesesMin, mesesMax,
                sexo != null ? sexo.name() : null);

        YearMonth mesAtual = YearMonth.now();

        return new DashboardGeralDto(
                avaliacoes.size(),
                avaliacoes.stream().map(a -> a.getPaciente().getId()).distinct().count(),
                avaliacoes.stream()
                        .filter(a -> YearMonth.from(a.getDataAvaliacao()).equals(mesAtual))
                        .count(),

                media(avaliacoes, a -> BigDecimal.valueOf(a.getIdadeMeses()), 1),
                media(avaliacoes, AvaliacaoPediatrica::getPeso, 2),
                media(avaliacoes, AvaliacaoPediatrica::getImc, 2),
                percentualAdequado(avaliacoes),
                media(avaliacoes, AvaliacaoPediatrica::getPercCalorico, 1),

                serieMensal(avaliacoes),

                distribuicao(avaliacoes, AvaliacaoPediatrica::getClassifPesoIdade, IndiceOms.PESO_IDADE),
                distribuicao(avaliacoes, AvaliacaoPediatrica::getClassifEstaturaIdade, IndiceOms.ESTATURA_IDADE),
                distribuicao(avaliacoes, AvaliacaoPediatrica::getClassifImcIdade, IndiceOms.IMC_IDADE),

                contar(avaliacoes, a -> a.getFormulaNome() == null ? "Sem fórmula" : a.getFormulaNome()),
                faixasEtarias(avaliacoes),
                contar(avaliacoes, a -> a.getSexo() == Sexo.MASCULINO ? "Masculino" : "Feminino"),

                ranking(avaliacoes));
    }

    /**
     * Percentual das avaliações COM classificação de IMC que caíram na faixa
     * adequada. Quem não tem IMC fica fora do numerador e do denominador — não
     * classificado não é sinônimo de inadequado.
     */
    private BigDecimal percentualAdequado(List<AvaliacaoPediatrica> avaliacoes) {
        List<FaixaOms> classificadas = avaliacoes.stream()
                .map(AvaliacaoPediatrica::getClassifImcIdade)
                .filter(Objects::nonNull)
                .toList();

        if (classificadas.isEmpty()) return null;

        long adequadas = classificadas.stream().filter(f -> f == FaixaOms.ADEQUADA).count();
        return BigDecimal.valueOf(adequadas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(classificadas.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal media(List<AvaliacaoPediatrica> avaliacoes,
                             Function<AvaliacaoPediatrica, BigDecimal> valor, int escala) {
        List<BigDecimal> valores = avaliacoes.stream()
                .map(valor)
                .filter(Objects::nonNull)
                .toList();

        if (valores.isEmpty()) return null;

        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), escala, RoundingMode.HALF_UP);
    }

    /** Série contínua: mês sem avaliação aparece com zero, senão o gráfico mente. */
    private List<PontoPeriodoDto> serieMensal(List<AvaliacaoPediatrica> avaliacoes) {
        Map<YearMonth, Long> porMes = avaliacoes.stream()
                .collect(Collectors.groupingBy(a -> YearMonth.from(a.getDataAvaliacao()),
                        Collectors.counting()));

        YearMonth fim = YearMonth.now();
        YearMonth inicio = porMes.keySet().stream().min(YearMonth::compareTo)
                .orElse(fim.minusMonths(MESES_NA_SERIE - 1L));
        if (inicio.isBefore(fim.minusMonths(MESES_NA_SERIE - 1L)))
            inicio = fim.minusMonths(MESES_NA_SERIE - 1L);

        List<PontoPeriodoDto> serie = new ArrayList<>();
        for (YearMonth m = inicio; !m.isAfter(fim); m = m.plusMonths(1))
            serie.add(new PontoPeriodoDto(m.atDay(1).format(MES), porMes.getOrDefault(m, 0L)));
        return serie;
    }

    /**
     * As três faixas sempre aparecem, mesmo zeradas — uma pizza que esconde a
     * fatia vazia faz parecer que ninguém está abaixo do peso.
     */
    private List<ContagemFaixaDto> distribuicao(List<AvaliacaoPediatrica> avaliacoes,
                                                Function<AvaliacaoPediatrica, FaixaOms> classificacao,
                                                IndiceOms indice) {
        Map<FaixaOms, Long> contagem = avaliacoes.stream()
                .map(classificacao)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<ContagemFaixaDto> saida = new ArrayList<>();
        for (FaixaOms faixa : FaixaOms.values())
            saida.add(new ContagemFaixaDto(faixa, indice.rotulo(faixa), contagem.getOrDefault(faixa, 0L)));

        long semClassificacao = avaliacoes.stream().map(classificacao).filter(Objects::isNull).count();
        if (semClassificacao > 0)
            saida.add(new ContagemFaixaDto(null, "Não classificado", semClassificacao));

        return saida;
    }

    private List<ContagemDto> faixasEtarias(List<AvaliacaoPediatrica> avaliacoes) {
        long[] contagem = new long[FAIXA_ROTULOS.length];

        for (AvaliacaoPediatrica a : avaliacoes) {
            int indice = FAIXA_LIMITES.length;
            for (int i = 0; i < FAIXA_LIMITES.length; i++) {
                if (a.getIdadeMeses() < FAIXA_LIMITES[i]) { indice = i; break; }
            }
            contagem[indice]++;
        }

        List<ContagemDto> saida = new ArrayList<>();
        for (int i = 0; i < FAIXA_ROTULOS.length; i++)
            saida.add(new ContagemDto(FAIXA_ROTULOS[i], contagem[i]));
        return saida;
    }

    private List<ContagemDto> contar(List<AvaliacaoPediatrica> avaliacoes,
                                     Function<AvaliacaoPediatrica, String> chave) {
        return avaliacoes.stream()
                .collect(Collectors.groupingBy(chave, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ContagemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ContagemDto::quantidade).reversed())
                .toList();
    }

    private List<PacienteRankingDto> ranking(List<AvaliacaoPediatrica> avaliacoes) {
        return avaliacoes.stream()
                .collect(Collectors.groupingBy(AvaliacaoPediatrica::getPaciente, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new PacienteRankingDto(e.getKey().getId(), e.getKey().getNome(), e.getValue()))
                .sorted(Comparator.comparingLong(PacienteRankingDto::avaliacoes).reversed()
                        .thenComparing(PacienteRankingDto::pacienteNome))
                .limit(10)
                .toList();
    }

    /** {@code dias = 0} significa "desde sempre" — sem corte inferior. */
    private LocalDate desde(int dias) {
        if (dias <= 0) return null;
        return LocalDate.now().minusDays(dias);
    }
}
