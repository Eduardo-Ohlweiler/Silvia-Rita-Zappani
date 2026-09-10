package com.nutri.hospitalar.clinica.service;

import com.nutri.hospitalar.clinica.dtos.CampoFichaDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaCreateDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaResponseDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaSelectDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaUpdateDto;
import com.nutri.hospitalar.clinica.entity.CampoFicha;
import com.nutri.hospitalar.clinica.entity.ModeloFicha;
import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;
import com.nutri.hospitalar.clinica.escore.EscalaNutricional;
import com.nutri.hospitalar.clinica.escore.EscalaRegistry;
import com.nutri.hospitalar.clinica.mapper.ModeloFichaMapper;
import com.nutri.hospitalar.clinica.mapper.OpcoesJson;
import com.nutri.hospitalar.clinica.mapper.PontosJson;
import com.nutri.hospitalar.clinica.repository.ModeloFichaRepository;
import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Modelos de ficha — o catálogo híbrido que sustenta a anamnese.
 *
 * <p><b>O do sistema é imutável.</b> Toda escrita passa por
 * {@link #buscarEditavel}, que procura só dentro do tenant: um modelo global não
 * é encontrado, e a resposta é <b>404</b>. Não é 403 de propósito — 403
 * confirmaria a existência do registro a quem não pode tocá-lo, e o resto do
 * sistema responde 404 nessa situação.
 *
 * <p>O caminho para adaptar um modelo do sistema é {@link #clonar}: a cópia
 * nasce do tenant, com as mesmas perguntas, e a partir daí é do cliente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeloFichaService {

    private final ModeloFichaRepository modeloFichaRepository;
    private final SecurityUtils securityUtils;

    // ── Leitura ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ModeloFichaResponseDto> getAll(Pageable pageable, String nome,
                                               Boolean ativo, Boolean doSistema) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return modeloFichaRepository
                .findAllWithFilters(PageableUtils.semOrdenacao(pageable),
                        tenantId, nome, ativo, doSistema)
                .map(ModeloFichaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ModeloFichaSelectDto> select(String termo) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return modeloFichaRepository.findForSelect(tenantId, termo).stream()
                .map(ModeloFichaMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModeloFichaResponseDto findById(UUID id) {
        return ModeloFichaMapper.toResponse(buscarVisivel(id));
    }

    /** Leitura: enxerga o do tenant e o do sistema. Usado também pela ficha. */
    @Transactional(readOnly = true)
    public ModeloFicha buscarVisivel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return modeloFichaRepository.findByIdVisivelPara(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Modelo de ficha não encontrado"));
    }

    // ── Escrita ─────────────────────────────────────────────────────────────

    @Transactional
    public ModeloFichaResponseDto create(ModeloFichaCreateDto dto) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        String nome = dto.nome().trim();

        if (modeloFichaRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, nome))
            throw new ConflictException("Já existe um modelo com esse nome");

        ModeloFicha modelo = new ModeloFicha();
        /* Sempre do tenant: não há como criar modelo do sistema pela API. */
        modelo.setTenant(securityUtils.getTenantReference());
        /* E sempre SEM escala: escoreCodigo não é aceito de fora. Um modelo de
           três perguntas que se declarasse MNA receberia as faixas da MNA sobre
           um total de cinco pontos, e a classificação sairia errada com cara de
           certa. A escala vem do seed, e só o clone a herda. */
        modelo.setNome(nome);
        modelo.setDescricao(textoOuNulo(dto.descricao()));
        modelo.setAtivo(dto.ativo() == null || dto.ativo());

        sincronizarCampos(modelo, dto.campos());

        ModeloFicha salvo = modeloFichaRepository.save(modelo);
        log.info("Modelo de ficha criado id={} campos={}", salvo.getId(), salvo.getCampos().size());
        return ModeloFichaMapper.toResponse(salvo);
    }

    @Transactional
    public ModeloFichaResponseDto update(UUID id, ModeloFichaUpdateDto dto) {
        ModeloFicha modelo = buscarEditavel(id);
        UUID tenantId = modelo.getTenant().getId();
        String nome = dto.nome().trim();

        if (modeloFichaRepository.existsByTenantIdAndNomeIgnoreCaseAndIdNot(tenantId, nome, id))
            throw new ConflictException("Já existe um modelo com esse nome");

        modelo.setNome(nome);
        modelo.setDescricao(textoOuNulo(dto.descricao()));
        if (dto.ativo() != null) modelo.setAtivo(dto.ativo());

        sincronizarCampos(modelo, dto.campos());

        log.info("Modelo de ficha alterado id={} campos={}", id, modelo.getCampos().size());
        return ModeloFichaMapper.toResponse(modeloFichaRepository.save(modelo));
    }

    /**
     * Cria uma cópia <b>do tenant</b> a partir de qualquer modelo visível.
     *
     * <p>É como o cliente parte de um modelo do sistema sem poder estragá-lo — e
     * é a única razão de os três semeados poderem ser imutáveis sem atrapalhar
     * ninguém.
     *
     * <p>O nome ganha um sufixo até ficar livre. Recusar com 409 seria correto e
     * inútil: quem clica em Clonar quer a cópia, não uma conversa sobre nomes.
     */
    @Transactional
    public ModeloFichaResponseDto clonar(UUID id) {
        ModeloFicha origem = buscarVisivel(id);
        UUID tenantId = securityUtils.getTenantIdLogado();

        ModeloFicha copia = new ModeloFicha();
        copia.setTenant(securityUtils.getTenantReference());
        copia.setNome(nomeLivreParaCopia(origem.getNome(), tenantId));
        copia.setDescricao(origem.getDescricao());
        copia.setAtivo(true);
        /* A escala acompanha a cópia. Uma MNA clonada que perdesse o escore
           viraria um questionário de dezoito perguntas sem propósito — e clonar
           é o único caminho para adaptar um modelo do sistema. O que protege a
           aritmética depois é validarEscala, no PUT. */
        copia.setEscoreCodigo(origem.getEscoreCodigo());

        origem.getCampos().stream()
                .sorted(Comparator.comparing(CampoFicha::getOrdem))
                .forEach(c -> {
                    CampoFicha novo = new CampoFicha();
                    novo.setSecao(c.getSecao());
                    novo.setRotulo(c.getRotulo());
                    novo.setTipo(c.getTipo());
                    novo.setOpcoes(c.getOpcoes());
                    novo.setPontos(c.getPontos());
                    novo.setGrupoEscore(c.getGrupoEscore());
                    novo.setOrdem(c.getOrdem());
                    novo.setObrigatorio(c.getObrigatorio());
                    novo.setAtivo(c.getAtivo());
                    copia.adicionarCampo(novo);
                });

        ModeloFicha salvo = modeloFichaRepository.save(copia);
        log.info("Modelo de ficha clonado origem={} copia={}", id, salvo.getId());
        return ModeloFichaMapper.toResponse(salvo);
    }

    @Transactional
    public ModeloFichaResponseDto alterarAtivo(UUID id, boolean ativo) {
        ModeloFicha modelo = buscarEditavel(id);
        modelo.setAtivo(ativo);

        log.info("Modelo de ficha {} alterado para ativo={}", id, ativo);
        return ModeloFichaMapper.toResponse(modeloFichaRepository.save(modelo));
    }

    /**
     * Apaga um modelo do cliente.
     *
     * <p><b>Não danifica ficha nenhuma</b>, e é consequência direta do retrato: a
     * ficha carrega o {@code modeloNome} e o texto integral de cada pergunta
     * respondida, e o {@code modelo_id} é anulável com
     * {@code ON DELETE SET NULL}. Sem o retrato, este método esvaziaria
     * prontuários.
     */
    @Transactional
    public void delete(UUID id) {
        ModeloFicha modelo = buscarEditavel(id);
        modeloFichaRepository.delete(modelo);
        log.info("Modelo de ficha excluído id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Escrita: só o do próprio tenant. O do sistema devolve 404 — ver a classe. */
    private ModeloFicha buscarEditavel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return modeloFichaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Modelo de ficha não encontrado"));
    }

    /**
     * A lista de campos é o <b>estado completo</b>, como os contatos de pessoa
     * (docs/08 §3.3): item com id atualiza, sem id cria, e o que não veio some.
     *
     * <p>O id que não for daquele modelo devolve 404 — a busca é no mapa dos
     * campos já carregados, e não no banco, justamente para que mandar o id de
     * outro modelo não altere nada.
     *
     * <p><b>A ordem é a posição na lista</b>, reatribuída aqui. Ordem digitada à
     * mão produz duas perguntas com o número 3 e uma tela que desempata sozinha.
     */
    private void sincronizarCampos(ModeloFicha modelo, List<CampoFichaDto> enviados) {
        Optional<EscalaNutricional> escala = EscalaRegistry.de(modelo.getEscoreCodigo());
        Map<UUID, CampoFicha> existentes = modelo.getCampos().stream()
                .collect(Collectors.toMap(CampoFicha::getId, Function.identity()));

        List<CampoFicha> resultado = new ArrayList<>();
        Set<String> perguntasVistas = new HashSet<>();

        int ordem = 1;
        for (CampoFichaDto dto : enviados) {
            validarCampo(dto, escala);

            if (!perguntasVistas.add(chaveDeDuplicata(dto)))
                throw new BadRequestException(
                        "A pergunta " + entreAspas(dto.rotulo()) + " está repetida na mesma seção");

            CampoFicha campo;
            if (dto.id() == null) {
                campo = new CampoFicha();
                campo.setModelo(modelo);
            } else {
                campo = existentes.get(dto.id());
                if (campo == null)
                    throw new NotFoundException("Pergunta não encontrada neste modelo");
            }

            campo.setSecao(textoOuNulo(dto.secao()));
            campo.setRotulo(dto.rotulo().trim());
            campo.setTipo(dto.tipo());
            campo.setOpcoes(dto.tipo().exigeOpcoes()
                    ? OpcoesJson.paraTexto(limpar(dto.opcoes()))
                    : null);
            campo.setPontos(PontosJson.paraTexto(dto.pontos()));
            campo.setGrupoEscore(textoOuNulo(dto.grupoEscore()));
            campo.setOrdem(ordem++);
            campo.setObrigatorio(dto.obrigatorio() != null && dto.obrigatorio());
            campo.setAtivo(dto.ativo() == null || dto.ativo());

            resultado.add(campo);
        }

        /* O que não veio foi removido pelo usuário: orphanRemoval o apaga. */
        modelo.getCampos().clear();
        modelo.getCampos().addAll(resultado);

        escala.ifPresent(e -> validarEscala(e, resultado));
    }

    /**
     * <b>Roda o limite contra os dados</b> antes de gravar um modelo com escala:
     * cada bloco tem de somar exatamente o máximo que a publicação declara.
     *
     * <p>É o que torna o clone seguro. Sem esta checagem, clonar a MNA e apagar
     * dez perguntas produziria um total máximo de 8 lido pelas faixas de 30 —
     * "desnutrido" para quem respondeu tudo, com cara de resultado. Reescrever o
     * enunciado de uma pergunta continua permitido, e é o que um cliente
     * legitimamente quer ao clonar: isso não move a aritmética.
     *
     * <p>Mesma lição do Codex contra o catálogo de fórmulas lácteas — a faixa
     * "óbvia" reprovava três das dez que o próprio sistema distribui. Aqui o seed
     * é reprovado por {@code seedCoerenteEscore} se alguém apertar a régua.
     */
    private void validarEscala(EscalaNutricional escala, List<CampoFicha> campos) {
        Map<String, BigDecimal> esperado = escala.maximoPorGrupo();
        Map<String, BigDecimal> obtido = new LinkedHashMap<>();

        for (CampoFicha campo : campos) {
            String grupo = campo.getGrupoEscore();
            if (grupo == null) continue;

            if (!esperado.containsKey(grupo))
                throw new BadRequestException("A escala " + escala.nome()
                        + " não tem o bloco " + entreAspas(grupo));

            obtido.merge(grupo, maiorPontoDe(campo), BigDecimal::add);
        }

        esperado.forEach((grupo, maximo) -> {
            BigDecimal soma = obtido.getOrDefault(grupo, BigDecimal.ZERO);
            if (soma.compareTo(maximo) != 0)
                throw new BadRequestException("As perguntas do bloco " + entreAspas(grupo)
                        + " somam no máximo " + soma.stripTrailingZeros().toPlainString()
                        + " pontos, e a escala " + escala.nome() + " exige "
                        + maximo.stripTrailingZeros().toPlainString()
                        + ". Reescrever o texto de uma pergunta é permitido; mudar a"
                        + " pontuação faria a classificação sair errada.");
        });
    }

    /** O teto de uma pergunta é a sua maior opção. Sem pontos, zero. */
    private BigDecimal maiorPontoDe(CampoFicha campo) {
        return PontosJson.paraLista(campo.getPontos()).stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Pergunta de opção <b>sem opções</b> é campo que não se pode responder, e
     * opção pendurada em campo de texto é lixo que reaparece se o tipo mudar — os
     * dois são recusados, e não ignorados em silêncio.
     */
    private void validarCampo(CampoFichaDto dto, Optional<EscalaNutricional> escala) {
        TipoCampoFicha tipo = dto.tipo();
        List<String> opcoes = limpar(dto.opcoes());
        List<BigDecimal> pontos = dto.pontos() == null ? List.of() : dto.pontos();

        if (tipo.exigeOpcoes()) {
            if (opcoes.size() < 2)
                throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                        + " precisa de ao menos duas opções");
            if (new HashSet<>(opcoes).size() != opcoes.size())
                throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                        + " tem opções repetidas");
        } else if (!opcoes.isEmpty()) {
            throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                    + " não é de opções e não aceita uma lista");
        }

        validarPontuacao(dto, tipo, opcoes, pontos, escala);
    }

    /**
     * As guardas da pontuação, e cada uma tem uma frase.
     *
     * <p>A da <b>cardinalidade</b> é a que o banco não faz: casar ponto com opção
     * por índice só é honesto quando as duas listas têm o mesmo comprimento, e um
     * CHECK que fizesse {@code cast} de texto para JSON derrubaria a linha inteira
     * diante de um {@code UPDATE} de manutenção malfeito — enquanto o
     * {@code PontosJson} foi escrito para nunca explodir ao ler. O banco garante o
     * que não pode variar; o service garante o que precisa de frase.
     *
     * <p>{@code MULTIPLAS_OPCOES} pontuado é recusado: nas duas escalas cada item
     * é escolha única, e somar N opções marcadas abre uma aritmética que nenhuma
     * publicação define.
     */
    private void validarPontuacao(CampoFichaDto dto, TipoCampoFicha tipo, List<String> opcoes,
                                  List<BigDecimal> pontos, Optional<EscalaNutricional> escala) {

        boolean temPontos = !pontos.isEmpty();
        String grupo = textoOuNulo(dto.grupoEscore());

        if ((temPontos || grupo != null) && escala.isEmpty())
            throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                    + " tem pontuação, e este modelo não aplica uma escala pontuada");

        if (temPontos) {
            if (tipo != TipoCampoFicha.OPCOES)
                throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                        + " só pode pontuar se for de opção única");
            if (pontos.size() != opcoes.size())
                throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                        + " tem " + opcoes.size() + " opções e " + pontos.size()
                        + " pontos: cada opção precisa do seu");
            if (pontos.stream().anyMatch(p -> p.signum() < 0))
                throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                        + " não pode ter pontuação negativa");
            if (grupo == null)
                throw new BadRequestException("A pergunta " + entreAspas(dto.rotulo())
                        + " pontua e precisa dizer em qual bloco da escala ela soma");
        }
    }

    /** Duas perguntas iguais na mesma seção são engano de quem montou. */
    private String chaveDeDuplicata(CampoFichaDto dto) {
        String secao = dto.secao() == null ? "" : dto.secao().trim().toLowerCase();
        return secao + "|" + dto.rotulo().trim().toLowerCase();
    }

    private List<String> limpar(List<String> opcoes) {
        if (opcoes == null) return List.of();
        return opcoes.stream()
                .filter(o -> o != null && !o.isBlank())
                .map(String::trim)
                .toList();
    }

    /** Anamnese X → Anamnese X (cópia), (cópia 2)… até achar um nome livre. */
    private String nomeLivreParaCopia(String base, UUID tenantId) {
        String candidato = truncar(base + " (cópia)");
        int n = 2;
        while (modeloFichaRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, candidato)) {
            candidato = truncar(base + " (cópia " + n + ")");
            n++;
        }
        return candidato;
    }

    private String truncar(String nome) {
        return nome.length() <= 200 ? nome : nome.substring(0, 200);
    }

    private String entreAspas(String texto) {
        return '"' + texto.trim() + '"';
    }

    private String textoOuNulo(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
