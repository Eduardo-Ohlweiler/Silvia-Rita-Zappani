package com.nutri.hospitalar.clinica.service;

import com.nutri.hospitalar.clinica.dtos.FichaAnamneseCreateDto;
import com.nutri.hospitalar.clinica.dtos.FichaAnamneseListaDto;
import com.nutri.hospitalar.clinica.dtos.FichaAnamneseResponseDto;
import com.nutri.hospitalar.clinica.dtos.FichaAnamneseUpdateDto;
import com.nutri.hospitalar.clinica.dtos.EscoreDto;
import com.nutri.hospitalar.clinica.dtos.EscoreRequestDto;
import com.nutri.hospitalar.clinica.dtos.RespostaFichaDto;
import com.nutri.hospitalar.clinica.entity.CampoFicha;
import com.nutri.hospitalar.clinica.entity.FichaAnamnese;
import com.nutri.hospitalar.clinica.entity.ModeloFicha;
import com.nutri.hospitalar.clinica.entity.RespostaFicha;
import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;
import com.nutri.hospitalar.clinica.escore.EscoreCalculator;
import com.nutri.hospitalar.clinica.mapper.EscoreJson;
import com.nutri.hospitalar.clinica.mapper.FichaAnamneseMapper;
import com.nutri.hospitalar.clinica.mapper.OpcoesJson;
import com.nutri.hospitalar.clinica.mapper.PontosJson;
import com.nutri.hospitalar.clinica.repository.FichaAnamneseRepository;
import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fichas de anamnese preenchidas.
 *
 * <p><b>Aqui mora o retrato.</b> Ao gravar, cada resposta copia da pergunta o
 * {@code secao}, {@code rotulo}, {@code tipo}, {@code opcoes}, {@code ordem} e
 * {@code obrigatorio}. Ler uma ficha salva nunca toca no modelo: a tela, o papel
 * e a planilha saem das linhas da própria ficha.
 *
 * <p>O modelo é consultado em dois momentos, e só neles: ao <b>gravar</b>, para
 * tirar o retrato; e ao <b>abrir</b>, para dizer se ele mudou desde então — que é
 * informação sobre o modelo, não sobre a ficha, e por isso vai como aviso ao lado
 * e nunca substitui o que está gravado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FichaAnamneseService {

    private final FichaAnamneseRepository fichaAnamneseRepository;
    private final ModeloFichaService modeloFichaService;
    private final PessoaRepository pessoaRepository;
    private final SecurityUtils securityUtils;

    // ── Leitura ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<FichaAnamneseListaDto> getAll(Pageable pageable, UUID pacienteId,
                                              UUID modeloId, LocalDate de, LocalDate ate) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        Page<FichaAnamnese> pagina = fichaAnamneseRepository.findAllWithFilters(
                PageableUtils.semOrdenacao(pageable), tenantId, pacienteId, modeloId, de, ate);

        if (pagina.isEmpty()) return pagina.map(f -> FichaAnamneseMapper.toLista(f, 0, 0));

        /* Uma consulta para a página inteira, e não uma por linha. */
        Map<UUID, int[]> contagens = contagensDe(pagina.getContent());

        return pagina.map(ficha -> {
            int[] c = contagens.getOrDefault(ficha.getId(), new int[]{0, 0});
            return FichaAnamneseMapper.toLista(ficha, c[1], c[0]);
        });
    }

    /**
     * Abre a ficha como ela foi gravada, e diz se o modelo mudou desde então.
     *
     * <p>Os dois avisos são conversas diferentes: <b>removido</b> é "o modelo não
     * existe mais, e esta ficha continua inteira"; <b>alterado</b> é "o modelo
     * ainda existe, mas não é mais este". O segundo engana mais, porque o combo
     * continua mostrando o nome certo.
     */
    @Transactional(readOnly = true)
    public FichaAnamneseResponseDto findById(UUID id) {
        FichaAnamnese ficha = buscar(id);
        ModeloFicha modelo = ficha.getModelo();

        boolean removido = modelo == null;
        boolean alterado = !removido && modeloDivergeDoRetrato(ficha, modelo);

        return FichaAnamneseMapper.toResponse(ficha, removido, alterado);
    }

    // ── Escrita ─────────────────────────────────────────────────────────────

    @Transactional
    public FichaAnamneseResponseDto create(FichaAnamneseCreateDto dto) {
        FichaAnamnese ficha = new FichaAnamnese();
        ficha.setTenant(securityUtils.getTenantReference());
        ficha.setCreatedBy(securityUtils.getUsuarioLogado());

        aplicar(ficha, dto.pacienteId(), dto.profissionalId(), dto.dataPreenchimento(),
                dto.modeloId(), dto.respostas(), dto.observacao());

        FichaAnamnese salva = fichaAnamneseRepository.save(ficha);
        log.info("Ficha de anamnese criada id={} respostas={}",
                salva.getId(), salva.getRespostas().size());

        return FichaAnamneseMapper.toResponse(salva, false, false);
    }

    /**
     * Regravar <b>atualiza o retrato</b> a partir dos campos vivos: é o mesmo
     * encontro sendo corrigido, com quem corrige vendo o que o modelo pergunta
     * hoje. O que nunca acontece é o retrato mudar <b>sem</b> alguém regravar.
     */
    @Transactional
    public FichaAnamneseResponseDto update(UUID id, FichaAnamneseUpdateDto dto) {
        FichaAnamnese ficha = buscar(id);
        ficha.setUpdatedBy(securityUtils.getUsuarioLogado());

        aplicar(ficha, dto.pacienteId(), dto.profissionalId(), dto.dataPreenchimento(),
                dto.modeloId(), dto.respostas(), dto.observacao());

        FichaAnamnese salva = fichaAnamneseRepository.save(ficha);
        log.info("Ficha de anamnese alterada id={} respostas={}", id, salva.getRespostas().size());

        return FichaAnamneseMapper.toResponse(salva, false, false);
    }

    @Transactional
    public void delete(UUID id) {
        FichaAnamnese ficha = buscar(id);
        fichaAnamneseRepository.delete(ficha);
        log.info("Ficha de anamnese excluída id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private FichaAnamnese buscar(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return fichaAnamneseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Ficha de anamnese não encontrada"));
    }

    private void aplicar(FichaAnamnese ficha, UUID pacienteId, UUID profissionalId,
                         LocalDate data, UUID modeloId, List<RespostaFichaDto> respostas,
                         String observacao) {

        ModeloFicha modelo = modeloFichaService.buscarVisivel(modeloId);

        ficha.setPaciente(buscarPessoa(pacienteId, "Paciente não encontrado"));
        ficha.setProfissional(profissionalId == null ? null
                : buscarPessoa(profissionalId, "Profissional não encontrado"));
        ficha.setDataPreenchimento(data);
        ficha.setModelo(modelo);
        /* O retrato do nome, que sobrevive a apagar o modelo. */
        ficha.setModeloNome(modelo.getNome());
        ficha.setObservacao(textoOuNulo(observacao));

        gravarRespostas(ficha, modelo, respostas);
    }

    /**
     * Monta as respostas <b>a partir das perguntas do modelo</b>, e não do que o
     * cliente mandou: a lista de perguntas é do servidor, e o cliente só diz o
     * valor de cada uma. Aceitar rótulo do front abriria caminho para uma ficha
     * afirmar ter perguntado o que o modelo não perguntava.
     *
     * <p>Só as perguntas <b>ativas</b> entram. Desativar uma pergunta no modelo
     * não apaga a resposta de fichas antigas — o retrato delas já está gravado —,
     * mas para de fazê-la em fichas novas.
     */
    private void gravarRespostas(FichaAnamnese ficha, ModeloFicha modelo,
                                 List<RespostaFichaDto> enviadas) {

        List<CampoFicha> perguntas = modelo.getCampos().stream()
                .filter(CampoFicha::getAtivo)
                .sorted(Comparator.comparing(CampoFicha::getOrdem))
                .toList();

        Map<UUID, String> valorPorCampo = valoresEnviados(enviadas, perguntas);

        /* Trocar o modelo descarta as respostas antigas e recomeça: meio caminho
           seria uma ficha com metade das perguntas de um modelo e metade de
           outro. O orphanRemoval apaga as que saíram. */
        ficha.getRespostas().clear();

        for (CampoFicha pergunta : perguntas) {
            String valor = normalizar(valorPorCampo.get(pergunta.getId()));
            validarResposta(pergunta, valor);

            RespostaFicha resposta = new RespostaFicha();
            resposta.setTenant(ficha.getTenant());
            resposta.setCampo(pergunta);

            // ─── O RETRATO ───────────────────────────────────────────────
            resposta.setSecao(pergunta.getSecao());
            resposta.setRotulo(pergunta.getRotulo());
            resposta.setTipo(pergunta.getTipo());
            resposta.setOpcoes(pergunta.getOpcoes());
            resposta.setOrdem(pergunta.getOrdem());
            resposta.setObrigatorio(pergunta.getObrigatorio());
            /* O ponto é retrato como o rótulo é: editar a pontuação do modelo
               não pode mexer no escore de uma ficha já gravada. */
            resposta.setPontos(PontosJson.pontoDe(
                    pergunta.getOpcoes(), pergunta.getPontos(), valor));
            resposta.setGrupoEscore(pergunta.getGrupoEscore());

            resposta.setValor(valor);

            ficha.adicionarResposta(resposta);
        }

        congelarEscore(ficha, modelo, perguntas, valorPorCampo);
    }

    /**
     * Calcula o escore e o <b>congela</b> na ficha.
     *
     * <p>Por que congelar, e não recalcular ao abrir: o retrato já congela as
     * entradas, mas {@code pessoa.data_nascimento} é editável fora da ficha —
     * corrigi-la faria o ponto por idade da NRS-2002 entrar ou sair, calado, em
     * toda ficha antiga daquele paciente —, e uma faixa corrigida em Java
     * reclassificaria prontuário retroativamente. Ver docs/13 §5.
     *
     * <p>As colunas planas acompanham o JSON porque JSON não se ordena nem se
     * filtra na native query da listagem.
     */
    private void congelarEscore(FichaAnamnese ficha, ModeloFicha modelo,
                                List<CampoFicha> perguntas, Map<UUID, String> valores) {

        EscoreDto escore = EscoreCalculator.avaliar(
                modelo.getEscoreCodigo(), perguntas, valores,
                idadeDoPaciente(ficha));

        ficha.setEscoreCodigo(escore == null ? null : escore.escala());
        ficha.setEscoreTotal(escore == null ? null : escore.total());
        ficha.setEscoreClassificacao(escore == null || escore.classificacao() == null
                ? null : escore.classificacao().rotulo());
        ficha.setEscoreTom(escore == null || escore.classificacao() == null
                ? null : escore.classificacao().tom().name());
        ficha.setEscoreJson(EscoreJson.paraTexto(escore));
    }

    /**
     * Anos completos <b>na data de preenchimento</b> — a ficha é o registro de um
     * dia, e uma ficha de dois anos atrás não pode ganhar hoje o ponto por idade
     * que o paciente só fez depois.
     */
    private Integer idadeDoPaciente(FichaAnamnese ficha) {
        Pessoa paciente = ficha.getPaciente();
        return paciente == null ? null
                : EscoreCalculator.idadeEm(paciente.getDataNascimento(),
                        ficha.getDataPreenchimento());
    }

    /**
     * O escore de um formulário <b>ainda não salvo</b>.
     *
     * <p>Mesma classe de cálculo da gravação — {@link EscoreCalculator} é a única
     * porta pela qual um escore é calculado neste sistema. Não há somador no
     * front, e isso não é economia de código: a regra do denominador da adesão
     * chegou a existir em quatro linguagens neste projeto, e sete telas erraram.
     *
     * <p><b>Nunca recusa por formulário incompleto.</b> Esta superfície recalcula
     * sozinha a cada pausa de digitação, e incompleto é <i>estado</i>, não erro —
     * a resposta vem 200 com escore nulo e o motivo escrito. Por isso também não
     * roda {@code validarResposta}: um número pela metade não pode virar 400 numa
     * tela que ainda está sendo preenchida.
     */
    @Transactional(readOnly = true)
    public EscoreDto calcularEscore(EscoreRequestDto dto) {
        ModeloFicha modelo = modeloFichaService.buscarVisivel(dto.modeloId());

        List<CampoFicha> perguntas = modelo.getCampos().stream()
                .filter(CampoFicha::getAtivo)
                .sorted(Comparator.comparing(CampoFicha::getOrdem))
                .toList();

        /* Id de pergunta que não é deste modelo é IGNORADO aqui, ao contrário da
           gravação, que devolve 404. A tela troca de modelo e o debounce ainda
           dispara com as respostas do anterior por alguns milissegundos: um 404
           nesse instante viraria um toast vermelho por causa de uma corrida que o
           usuário nem percebeu. Na gravação o rigor continua inteiro. */
        Set<UUID> doModelo = perguntas.stream().map(CampoFicha::getId)
                .collect(Collectors.toSet());

        Map<UUID, String> valores = new HashMap<>();
        if (dto.respostas() != null) {
            for (RespostaFichaDto resposta : dto.respostas()) {
                if (doModelo.contains(resposta.campoId()))
                    valores.put(resposta.campoId(), normalizar(resposta.valor()));
            }
        }

        return EscoreCalculator.avaliar(modelo.getEscoreCodigo(), perguntas, valores,
                idadeParaEscore(dto));
    }

    /**
     * Sem paciente escolhido ainda, a idade é desconhecida — e a NRS-2002 diz
     * isso por escrito em vez de somar zero. A data em branco vale como hoje: o
     * formulário abre com ela preenchida, e um instante sem data não pode
     * apagar o escore inteiro da tela.
     */
    private Integer idadeParaEscore(EscoreRequestDto dto) {
        if (dto.pacienteId() == null) return null;

        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findByIdAndTenantId(dto.pacienteId(), tenantId)
                .map(p -> EscoreCalculator.idadeEm(p.getDataNascimento(),
                        dto.dataPreenchimento() == null
                                ? LocalDate.now() : dto.dataPreenchimento()))
                .orElse(null);
    }

    /**
     * Um id de pergunta que não é deste modelo devolve <b>404</b>, e não é
     * ignorado: mandar o id de outro modelo não pode alterar nada em silêncio, e
     * quem o mandou tem um erro para corrigir.
     */
    private Map<UUID, String> valoresEnviados(List<RespostaFichaDto> enviadas,
                                              List<CampoFicha> perguntas) {
        if (enviadas == null || enviadas.isEmpty()) return Map.of();

        Set<UUID> idsDoModelo = perguntas.stream()
                .map(CampoFicha::getId)
                .collect(Collectors.toSet());

        Map<UUID, String> mapa = new HashMap<>();
        for (RespostaFichaDto dto : enviadas) {
            if (!idsDoModelo.contains(dto.campoId()))
                throw new NotFoundException("Pergunta não encontrada neste modelo");
            mapa.put(dto.campoId(), dto.valor());
        }
        return mapa;
    }

    /**
     * O servidor valida de novo, sempre — o zod do front só evita a ida e volta.
     *
     * <p>A mensagem <b>nomeia a pergunta</b>. "Campo obrigatório" numa ficha de
     * vinte perguntas manda o profissional procurar qual.
     */
    private void validarResposta(CampoFicha pergunta, String valor) {
        boolean vazio = valor == null;

        if (pergunta.getObrigatorio() && vazio)
            throw new BadRequestException("Responda a pergunta " + entreAspas(pergunta.getRotulo()));

        if (vazio) return;

        switch (pergunta.getTipo()) {
            case CHECKBOX -> {
                if (!"true".equals(valor) && !"false".equals(valor))
                    throw new BadRequestException("A pergunta " + entreAspas(pergunta.getRotulo())
                            + " só aceita sim ou não");
            }
            case NUMERO -> {
                try {
                    new BigDecimal(valor);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("A pergunta " + entreAspas(pergunta.getRotulo())
                            + " espera um número");
                }
            }
            case DATA -> {
                try {
                    LocalDate.parse(valor);
                } catch (DateTimeParseException e) {
                    throw new BadRequestException("A pergunta " + entreAspas(pergunta.getRotulo())
                            + " espera uma data");
                }
            }
            case OPCOES -> {
                List<String> permitidas = OpcoesJson.paraLista(pergunta.getOpcoes());
                if (!permitidas.contains(valor))
                    throw new BadRequestException("A pergunta " + entreAspas(pergunta.getRotulo())
                            + " não oferece a opção escolhida");
            }
            case MULTIPLAS_OPCOES -> {
                List<String> permitidas = OpcoesJson.paraLista(pergunta.getOpcoes());
                List<String> escolhidas = OpcoesJson.paraLista(valor);
                if (escolhidas.isEmpty())
                    throw new BadRequestException("A pergunta " + entreAspas(pergunta.getRotulo())
                            + " espera uma lista de opções");
                if (!permitidas.containsAll(escolhidas))
                    throw new BadRequestException("A pergunta " + entreAspas(pergunta.getRotulo())
                            + " não oferece alguma das opções escolhidas");
            }
            case TEXTO, TEXTO_LONGO -> { /* texto livre não tem o que validar */ }
        }
    }

    /**
     * O modelo vivo ainda produz esta ficha?
     *
     * <p>Compara o retrato de cada resposta com a pergunta correspondente, e
     * também olha o outro lado: pergunta <b>nova e ativa</b> que a ficha não tem
     * também é alteração. Sem isso, acrescentar uma pergunta ao modelo passaria
     * despercebido, e a ficha pareceria completa faltando um dado.
     */
    private boolean modeloDivergeDoRetrato(FichaAnamnese ficha, ModeloFicha modelo) {
        Map<UUID, CampoFicha> vivos = modelo.getCampos().stream()
                .filter(CampoFicha::getAtivo)
                .collect(Collectors.toMap(CampoFicha::getId, c -> c));

        Set<UUID> retratados = new HashSet<>();

        for (RespostaFicha resposta : ficha.getRespostas()) {
            UUID campoId = resposta.getCampo() == null ? null : resposta.getCampo().getId();
            if (campoId == null) return true;          // a pergunta saiu do modelo

            CampoFicha vivo = vivos.get(campoId);
            if (vivo == null) return true;             // desativada ou apagada

            retratados.add(campoId);

            if (!Objects.equals(vivo.getRotulo(), resposta.getRotulo())) return true;
            if (!Objects.equals(vivo.getSecao(), resposta.getSecao())) return true;
            if (vivo.getTipo() != resposta.getTipo()) return true;
            if (!Objects.equals(vivo.getObrigatorio(), resposta.getObrigatorio())) return true;
            if (!OpcoesJson.paraLista(vivo.getOpcoes())
                    .equals(OpcoesJson.paraLista(resposta.getOpcoes()))) return true;
        }

        return !retratados.containsAll(vivos.keySet());  // pergunta nova entrou
    }

    /** {@code [total, respondidas]} por ficha, numa consulta só. */
    private Map<UUID, int[]> contagensDe(List<FichaAnamnese> fichas) {
        List<UUID> ids = fichas.stream().map(FichaAnamnese::getId).toList();

        Map<UUID, int[]> mapa = new HashMap<>();
        for (Object[] linha : fichaAnamneseRepository.contarRespostasPorFicha(ids)) {
            mapa.put((UUID) linha[0], new int[]{
                    ((Number) linha[1]).intValue(),
                    ((Number) linha[2]).intValue()});
        }
        return mapa;
    }

    /** Paciente e profissional são pessoas do próprio tenant — nunca de outro. */
    private Pessoa buscarPessoa(UUID id, String mensagem) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException(mensagem));
    }

    /**
     * Branco e nulo são a mesma coisa aqui: <b>não informado</b>. Guardar string
     * vazia faria um sim/não em branco parecer respondido na contagem da lista.
     */
    private String normalizar(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private String entreAspas(String texto) {
        return '"' + texto.trim() + '"';
    }

    private String textoOuNulo(String valor) {
        return normalizar(valor);
    }
}
