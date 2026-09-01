package com.nutri.hospitalar.pediatria.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pediatria.calculo.LinhaPercentil;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaSugeridaDto;
import com.nutri.hospitalar.pediatria.dtos.MedidasDoDiaPediatrico;
import com.nutri.hospitalar.pediatria.dtos.PainelAcompanhamentoPediatricoDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoCreateDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoListaDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoResponseDto;
import com.nutri.hospitalar.pediatria.dtos.RegistroDiarioPediatricoUpdateDto;
import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
import com.nutri.hospitalar.pediatria.entity.RegistroDiarioPediatrico;
import com.nutri.hospitalar.pediatria.mapper.RegistroDiarioPediatricoMapper;
import com.nutri.hospitalar.pediatria.repository.AvaliacaoPediatricaRepository;
import com.nutri.hospitalar.pediatria.repository.RegistroDiarioPediatricoRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O acompanhamento diário pediátrico. Especificação em {@code docs/11}.
 *
 * <p><b>Um registro por paciente por dia</b>, garantido pela {@code UNIQUE} do
 * banco e antecipado aqui com uma mensagem que diz qual dia.
 *
 * <p><b>O vínculo com a avaliação nunca acontece em silêncio.</b> A tela pede a
 * sugestão em {@link #avaliacaoSugerida} e manda o que o usuário confirmar.
 * Ligar sozinho faria a adequação calórica mudar sem que ninguém tivesse
 * escolhido a referência.
 *
 * <p><b>A linha da OMS é resolvida aqui, em lote.</b> O calculador é puro; e como
 * cada registro classifica com a idade <b>do seu próprio dia</b>, uma lista de 90
 * dias precisaria de 90 consultas. O mapa por sexo resolve isso numa.
 *
 * <p><b>Nada de dado clínico em log</b> (regra 5): só o id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistroDiarioPediatricoService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String SEM_AVALIACAO_ATE_A_DATA =
            "Este paciente ainda não tem avaliação até esta data. O dia pode ser registrado "
                    + "assim mesmo — as adequações calórica e proteica ficam de fora.";

    private final RegistroDiarioPediatricoRepository registroDiarioPediatricoRepository;
    private final AvaliacaoPediatricaRepository avaliacaoPediatricaRepository;
    private final PessoaRepository pessoaRepository;
    private final CalculoPediatricoService calculoPediatricoService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<RegistroDiarioPediatricoListaDto> getAll(Pageable pageable, UUID pessoaId,
                                                         String pessoaNome,
                                                         LocalDate de, LocalDate ate) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        Page<RegistroDiarioPediatrico> pagina = registroDiarioPediatricoRepository
                .findAllWithFilters(PageableUtils.semOrdenacao(pageable),
                        tenantId, pessoaId, textoOuNulo(pessoaNome), de, ate);

        // Um mapa por sexo, não uma consulta por linha: a lista mistura
        // pacientes, e cada um traz o seu.
        Curvas curvas = new Curvas();
        return pagina.map(r -> RegistroDiarioPediatricoMapper.toLista(r, curvas.linhaPara(r)));
    }

    @Transactional(readOnly = true)
    public RegistroDiarioPediatricoResponseDto findById(UUID id) {
        RegistroDiarioPediatrico registro = buscar(id);
        return RegistroDiarioPediatricoMapper.toResponse(registro, new Curvas().linhaPara(registro));
    }

    /**
     * Qual avaliação o dia deveria referenciar — <b>sugestão</b>, não vínculo.
     *
     * <p>A mais recente daquele paciente <b>até</b> aquela data: um dia de três
     * meses atrás não deve ser comparado com a prescrição de ontem.
     */
    @Transactional(readOnly = true)
    public AvaliacaoPediatricaSugeridaDto avaliacaoSugerida(UUID pessoaId, LocalDate data) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return registroDiarioPediatricoRepository
                .findIdDaAvaliacaoVigente(tenantId, pessoaId, data)
                .flatMap(id -> avaliacaoPediatricaRepository.findByIdAndTenantId(id, tenantId))
                .map(a -> new AvaliacaoPediatricaSugeridaDto(
                        a.getId(), a.getDataAvaliacao(), a.getPeso(),
                        a.getVolumeTotal(), a.getVet(), a.getProteinaNecessidade(),
                        a.getFormulaNome(), null))
                .orElseGet(() -> new AvaliacaoPediatricaSugeridaDto(
                        null, null, null, null, null, null, null, SEM_AVALIACAO_ATE_A_DATA));
    }

    @Transactional
    public RegistroDiarioPediatricoResponseDto create(RegistroDiarioPediatricoCreateDto dto) {
        recusarDiaRepetido(dto.pessoaId(), dto.data(), null);

        RegistroDiarioPediatrico registro = new RegistroDiarioPediatrico();
        registro.setTenant(securityUtils.getTenantReference());
        registro.setCreatedBy(securityUtils.getUsuarioLogado());
        registro.setPessoa(buscarPessoa(dto.pessoaId()));
        registro.setData(dto.data());
        registro.setAvaliacao(resolverAvaliacao(dto.avaliacaoId()));

        aplicarMedidas(registro, dto);

        RegistroDiarioPediatrico salvo = registroDiarioPediatricoRepository.save(registro);
        log.info("Registro diário pediátrico criado id={}", salvo.getId());
        return RegistroDiarioPediatricoMapper.toResponse(salvo, new Curvas().linhaPara(salvo));
    }

    @Transactional
    public RegistroDiarioPediatricoResponseDto update(UUID id,
                                                      RegistroDiarioPediatricoUpdateDto dto) {
        RegistroDiarioPediatrico registro = buscar(id);
        recusarDiaRepetido(dto.pessoaId(), dto.data(), id);

        registro.setUpdatedBy(securityUtils.getUsuarioLogado());
        registro.setPessoa(buscarPessoa(dto.pessoaId()));
        registro.setData(dto.data());
        registro.setAvaliacao(resolverAvaliacao(dto.avaliacaoId()));

        aplicarMedidas(registro, dto);

        RegistroDiarioPediatrico salvo = registroDiarioPediatricoRepository.save(registro);
        log.info("Registro diário pediátrico alterado id={}", id);
        return RegistroDiarioPediatricoMapper.toResponse(salvo, new Curvas().linhaPara(salvo));
    }

    @Transactional
    public void remover(UUID id) {
        registroDiarioPediatricoRepository.delete(buscar(id));
        log.info("Registro diário pediátrico removido id={}", id);
    }

    /**
     * O painel de acompanhamento de um paciente — o terceiro painel da
     * pediatria, que só existe porque agora há eixo do tempo.
     *
     * <p>Os dias vêm inteiros, na mesma forma que a tela do acompanhamento
     * consome: uma forma própria de ponto duplicaria os derivados, e no dia em
     * que divergisse o gráfico e a lista mostrariam números diferentes do mesmo
     * dia.
     *
     * <p><b>A janela pedida é respeitada</b>, não os dias que têm dado — a
     * armadilha da série temporal já registrada no {@code CLAUDE.md}. Quem pede
     * 30 dias recebe a janela de 30 dias, com os dias que existirem dentro dela.
     */
    @Transactional(readOnly = true)
    public PainelAcompanhamentoPediatricoDto painelAcompanhamento(UUID pessoaId,
                                                                  LocalDate de, LocalDate ate) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        Pessoa paciente = buscarPessoa(pessoaId);

        List<RegistroDiarioPediatrico> registros = registroDiarioPediatricoRepository
                .findParaPainel(tenantId, pessoaId, de, ate);

        Curvas curvas = new Curvas();
        List<RegistroDiarioPediatricoResponseDto> dias = registros.stream()
                .map(r -> RegistroDiarioPediatricoMapper.toResponse(r, curvas.linhaPara(r)))
                .toList();

        // O peso do primeiro e do último dia QUE TÊM peso medido — não do
        // primeiro e do último dia do período. Um dia sem balança no fim da
        // janela zeraria o ganho de duas semanas.
        List<RegistroDiarioPediatricoResponseDto> comPeso = dias.stream()
                .filter(d -> d.pesoKg() != null)
                .toList();

        BigDecimal pesoInicial = comPeso.isEmpty() ? null : comPeso.getFirst().pesoKg();
        BigDecimal pesoFinal = comPeso.isEmpty() ? null : comPeso.getLast().pesoKg();
        BigDecimal variacao = pesoInicial == null || pesoFinal == null
                ? null : pesoFinal.subtract(pesoInicial);

        AvaliacaoPediatrica ultima = registros.stream()
                .map(RegistroDiarioPediatrico::getAvaliacao)
                .filter(java.util.Objects::nonNull)
                .reduce((a, b) -> b)
                .orElse(null);

        return new PainelAcompanhamentoPediatricoDto(
                paciente.getId(), paciente.getNome(),
                de, ate,
                dias.size(),
                dias.stream().filter(d -> d.avaliacaoId() == null).count(),

                media(dias, d -> d.derivados().percentualRecebido()),
                media(dias, d -> d.derivados().caloriasPorKg()),
                media(dias, d -> d.derivados().proteinaPorKg()),
                media(dias, d -> d.derivados().adequacaoCalorica()),
                media(dias, d -> d.derivados().adequacaoProteica()),
                media(dias, d -> d.derivados().aceitacaoTomadas()),

                pesoInicial, pesoFinal, variacao,
                comPeso.isEmpty() ? null : comPeso.getFirst().derivados().idadeMeses(),
                comPeso.isEmpty() ? null : comPeso.getLast().derivados().idadeMeses(),

                ultima == null ? null : ultima.getDataAvaliacao(),
                ultima == null ? null : ultima.getVet(),
                ultima == null ? null : ultima.getProteinaNecessidade(),
                ultima == null ? null : ultima.getVolumeTotal(),

                dias);
    }

    /**
     * Média dos dias em que o valor <b>existe</b>.
     *
     * <p>Dia sem o valor não entra como zero: contar ausência como zero faria a
     * criança parecer pior do que está. É o erro que a planilha comete ao usar
     * {@code MÉDIA} sobre coluna com célula vazia formatada como zero.
     */
    private static BigDecimal media(List<RegistroDiarioPediatricoResponseDto> dias,
                                    java.util.function.Function<RegistroDiarioPediatricoResponseDto,
                                            BigDecimal> valor) {
        List<BigDecimal> presentes = dias.stream()
                .map(valor)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (presentes.isEmpty()) return null;

        BigDecimal soma = presentes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(new BigDecimal(presentes.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Quantos dias estão presos a esta avaliação.
     *
     * <p>Chamado pela exclusão de avaliação, para o 409 dizer o número em vez de
     * deixar o {@code ON DELETE RESTRICT} chegar cru ao usuário.
     */
    @Transactional(readOnly = true)
    public long contarDiasDaAvaliacao(UUID avaliacaoId) {
        return registroDiarioPediatricoRepository.countByTenantIdAndAvaliacaoId(
                securityUtils.getTenantIdLogado(), avaliacaoId);
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * As curvas da OMS carregadas sob demanda, um sexo por vez.
     *
     * <p><b>Por que existe:</b> cada registro classifica com a idade do seu
     * próprio dia ({@code docs/11} §3), então a linha da tabela muda de registro
     * para registro — mas só há <b>duas</b> tabelas, uma por sexo, com 61 linhas
     * cada. Carregar a tabela do sexo na primeira vez que ele aparece troca N
     * consultas por, no máximo, duas.
     */
    private final class Curvas {

        private final Map<Sexo, Map<Integer, LinhaPercentil>> porSexo = new EnumMap<>(Sexo.class);

        LinhaPercentil linhaPara(RegistroDiarioPediatrico r) {
            Sexo sexo = r.getPessoa().getSexo() != null
                    ? r.getPessoa().getSexo()
                    : (r.getAvaliacao() == null ? null : r.getAvaliacao().getSexo());

            Integer idade = RegistroDiarioPediatricoMapper.idadeNoDia(r.getPessoa(), r.getData());
            if (sexo == null || idade == null) return null;

            return porSexo
                    .computeIfAbsent(sexo, calculoPediatricoService::linhasPorIdade)
                    .get(idade);
        }
    }

    /**
     * Dois registros do mesmo paciente no mesmo dia não podem coexistir.
     *
     * <p>A {@code UNIQUE} do banco é a garantia real; isto existe para o usuário
     * receber "já existe um registro em 15/03/2026" em vez de uma violação de
     * constraint crua.
     */
    private void recusarDiaRepetido(UUID pessoaId, LocalDate data, UUID idAtual) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        registroDiarioPediatricoRepository
                .findByTenantIdAndPessoaIdAndData(tenantId, pessoaId, data)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new ConflictException(
                            "Já existe um acompanhamento deste paciente em %s. Abra o registro "
                                    + "existente em vez de criar outro.".formatted(data.format(DIA)));
                });
    }

    /** A avaliação, sempre dentro do tenant. Nulo é resposta válida. */
    private AvaliacaoPediatrica resolverAvaliacao(UUID avaliacaoId) {
        if (avaliacaoId == null) return null;

        UUID tenantId = securityUtils.getTenantIdLogado();
        return avaliacaoPediatricaRepository.findByIdAndTenantId(avaliacaoId, tenantId)
                .orElseThrow(() -> new NotFoundException("Avaliação não encontrada"));
    }

    /** Uma implementação só: os dois DTOs expõem as mesmas medidas. */
    private void aplicarMedidas(RegistroDiarioPediatrico r, MedidasDoDiaPediatrico d) {
        r.setPesoKg(d.pesoKg());
        r.setEstaturaCm(d.estaturaCm());
        r.setVolPrescrito24h(d.volPrescrito24h());
        r.setVolRecebido24h(d.volRecebido24h());
        r.setTomadasPrevistas(d.tomadasPrevistas());
        r.setTomadasAceitas(d.tomadasAceitas());
        r.setObservacao(textoOuNulo(d.observacao()));

        recusarAceitasAcimaDePrevistas(r);
    }

    /**
     * Aceitar <b>mais</b> tomadas do que se previu é erro de digitação.
     *
     * <p>O banco tem o {@code CHECK}, mas ele chegaria cru; e sem nenhum dos dois
     * a aceitação sairia acima de 100 % na tela, plausível e errada.
     */
    private void recusarAceitasAcimaDePrevistas(RegistroDiarioPediatrico r) {
        Integer previstas = r.getTomadasPrevistas();
        Integer aceitas = r.getTomadasAceitas();

        if (previstas != null && aceitas != null && aceitas > previstas)
            throw new ConflictException(
                    ("Foram registradas %d tomadas aceitas para %d previstas. Confira: "
                            + "aceitar mais do que se previu não é possível.")
                            .formatted(aceitas, previstas));
    }

    private RegistroDiarioPediatrico buscar(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return registroDiarioPediatricoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Acompanhamento não encontrado"));
    }

    private Pessoa buscarPessoa(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
