package com.nutri.hospitalar.uti.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalCreateDto;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalResponseDto;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalSelectDto;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalUpdateDto;
import com.nutri.hospitalar.uti.entity.ProdutoNutricional;
import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import com.nutri.hospitalar.uti.enums.TipoProdutoNutricional;
import com.nutri.hospitalar.uti.mapper.ProdutoNutricionalMapper;
import com.nutri.hospitalar.uti.repository.ProdutoNutricionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Catálogo de produtos nutricionais — suplemento oral, módulo proteico e insumo
 * de dieta artesanal.
 *
 * <p><b>Ler alcança os globais; escrever, não</b>, como em fórmula enteral e em
 * fórmula láctea.
 *
 * <p>O que este service tem de próprio é a <b>coerência entre tipo e papel</b>.
 * O banco já a impõe por {@code CHECK}, mas violação de constraint chega ao
 * usuário como erro de banco: aqui ela é conferida antes, com mensagem que diz
 * o que fazer. E {@code moduloProteico} é <b>derivado do tipo</b>, nunca aceito
 * do cliente — são o mesmo fato dito duas vezes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoNutricionalService {

    private static final String GLOBAL_NAO_EDITAVEL =
            "Este é um produto do sistema e não pode ser alterado. "
                    + "Cadastre um produto próprio se precisar de outra composição.";

    private final ProdutoNutricionalRepository produtoNutricionalRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<ProdutoNutricionalResponseDto> getAll(Pageable pageable, String nome,
                                                      TipoProdutoNutricional tipo,
                                                      Boolean ativo, Boolean global) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return produtoNutricionalRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, textoOuNulo(nome), nomeEnum(tipo), ativo, global)
                .map(ProdutoNutricionalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProdutoNutricionalSelectDto> select(TipoProdutoNutricional tipo, String termo) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return produtoNutricionalRepository
                .findForSelect(tenantId, nomeEnum(tipo), textoOuNulo(termo))
                .stream()
                .map(ProdutoNutricionalMapper::toSelect)
                .toList();
    }

    /**
     * Os módulos proteicos que a dieta enteral oferece.
     *
     * <p>É esta consulta que substitui a constante de três itens do eroERP: aqui
     * cadastrar um módulo passa a ter efeito na sugestão.
     */
    @Transactional(readOnly = true)
    public List<ProdutoNutricionalSelectDto> modulosProteicos() {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return produtoNutricionalRepository.findModulosProteicos(tenantId)
                .stream()
                .map(ProdutoNutricionalMapper::toSelect)
                .toList();
    }

    /** Os insumos de um papel da receita artesanal — o do tenant vem primeiro. */
    @Transactional(readOnly = true)
    public List<ProdutoNutricionalSelectDto> insumosPorPapel(PapelArtesanal papel) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return produtoNutricionalRepository.findInsumoPorPapel(tenantId, papel)
                .stream()
                .map(ProdutoNutricionalMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoNutricionalResponseDto findById(UUID id) {
        return ProdutoNutricionalMapper.toResponse(buscarVisivel(id));
    }

    /** Leitura: enxerga o do tenant e o global. Usado também pelo cálculo. */
    @Transactional(readOnly = true)
    public ProdutoNutricional buscarVisivel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return produtoNutricionalRepository.findByIdVisivelPara(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));
    }

    @Transactional
    public ProdutoNutricionalResponseDto create(ProdutoNutricionalCreateDto dto) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        String nome = dto.nome().trim();

        if (produtoNutricionalRepository
                .existsByTenantIdAndTipoAndNomeIgnoreCase(tenantId, dto.tipo(), nome))
            throw new ConflictException(
                    "Já existe um %s com esse nome".formatted(dto.tipo().getDescricao().toLowerCase()));

        validarCoerencia(dto.tipo(), dto.papelArtesanal(), dto.kcal(), dto.proteinaG());

        ProdutoNutricional produto = new ProdutoNutricional();
        produto.setTenant(securityUtils.getTenantReference());
        produto.setNome(nome);
        aplicar(produto, dto);
        produto.setAtivo(dto.ativo() == null || dto.ativo());

        ProdutoNutricional salvo = produtoNutricionalRepository.save(produto);
        log.info("Produto nutricional criado id={} tipo={}", salvo.getId(), salvo.getTipo());
        return ProdutoNutricionalMapper.toResponse(salvo);
    }

    @Transactional
    public ProdutoNutricionalResponseDto update(UUID id, ProdutoNutricionalUpdateDto dto) {
        ProdutoNutricional produto = buscarEditavel(id);
        UUID tenantId = produto.getTenant().getId();
        String nome = dto.nome().trim();

        if (produtoNutricionalRepository.existsByTenantIdAndTipoAndNomeIgnoreCaseAndIdNot(
                tenantId, dto.tipo(), nome, id))
            throw new ConflictException(
                    "Já existe um %s com esse nome".formatted(dto.tipo().getDescricao().toLowerCase()));

        validarCoerencia(dto.tipo(), dto.papelArtesanal(), dto.kcal(), dto.proteinaG());

        produto.setNome(nome);
        aplicar(produto, dto);

        log.info("Produto nutricional alterado id={}", id);
        return ProdutoNutricionalMapper.toResponse(produtoNutricionalRepository.save(produto));
    }

    @Transactional
    public ProdutoNutricionalResponseDto alterarAtivo(UUID id, boolean ativo) {
        ProdutoNutricional produto = buscarEditavel(id);
        produto.setAtivo(ativo);

        log.info("Produto nutricional {} alterado para ativo={}", id, ativo);
        return ProdutoNutricionalMapper.toResponse(produtoNutricionalRepository.save(produto));
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * As duas regras que o tipo impõe, com a mensagem que diz o que fazer.
     *
     * <p>Papel artesanal existe <b>se e só se</b> o tipo é insumo: papel em
     * suplemento não teria efeito nenhum, e insumo sem papel nunca entraria na
     * receita — ficaria cadastrado e invisível, que é justamente o defeito do
     * eroERP que este catálogo veio consertar.
     *
     * <p>E o que entra em cálculo precisa de composição: módulo proteico sem
     * kcal ou sem proteína produziria dose nula em silêncio.
     */
    private void validarCoerencia(TipoProdutoNutricional tipo, PapelArtesanal papel,
                                  BigDecimal kcal, BigDecimal proteinaG) {

        if (tipo == TipoProdutoNutricional.INSUMO_ARTESANAL && papel == null)
            throw new BadRequestException(
                    "Insumo de dieta artesanal precisa do papel na receita "
                            + "(base, carboidrato, proteína ou lipídio) — é o papel que faz o "
                            + "insumo entrar no cálculo.");

        if (tipo != TipoProdutoNutricional.INSUMO_ARTESANAL && papel != null)
            throw new BadRequestException(
                    ("Papel na receita artesanal só se aplica a insumo de dieta artesanal. "
                            + "Em %s ele não teria efeito.")
                            .formatted(tipo.getDescricao().toLowerCase()));

        if (tipo.exigeComposicao() && (kcal == null || proteinaG == null))
            throw new BadRequestException(
                    ("%s entra em cálculo e precisa de calorias e proteína por medida. "
                            + "Sem elas a dose sairia nula sem avisar.")
                            .formatted(tipo.getDescricao()));
    }

    private void aplicar(ProdutoNutricional produto, ProdutoNutricionalCreateDto dto) {
        aplicarComuns(produto, dto.tipo(), dto.medidaNome(), dto.medidaQtd(), dto.embalagemQtd(),
                dto.kcal(), dto.proteinaG(), dto.choG(), dto.acucarG(), dto.lipG(),
                dto.sodioMg(), dto.potassioMg(), dto.fosforoMg(), dto.ferroMg(),
                dto.fibrasG(), dto.osmolaridadeMosmL(), dto.papelArtesanal(), dto.observacao());
    }

    private void aplicar(ProdutoNutricional produto, ProdutoNutricionalUpdateDto dto) {
        aplicarComuns(produto, dto.tipo(), dto.medidaNome(), dto.medidaQtd(), dto.embalagemQtd(),
                dto.kcal(), dto.proteinaG(), dto.choG(), dto.acucarG(), dto.lipG(),
                dto.sodioMg(), dto.potassioMg(), dto.fosforoMg(), dto.ferroMg(),
                dto.fibrasG(), dto.osmolaridadeMosmL(), dto.papelArtesanal(), dto.observacao());
    }

    private void aplicarComuns(ProdutoNutricional produto, TipoProdutoNutricional tipo,
                               String medidaNome, BigDecimal medidaQtd, BigDecimal embalagemQtd,
                               BigDecimal kcal, BigDecimal proteinaG, BigDecimal choG,
                               BigDecimal acucarG, BigDecimal lipG, BigDecimal sodioMg,
                               BigDecimal potassioMg, BigDecimal fosforoMg, BigDecimal ferroMg,
                               BigDecimal fibrasG, BigDecimal osmolaridadeMosmL,
                               PapelArtesanal papelArtesanal, String observacao) {
        produto.setTipo(tipo);
        produto.setMedidaNome(medidaNome.trim());
        produto.setMedidaQtd(medidaQtd);
        produto.setEmbalagemQtd(embalagemQtd);
        produto.setKcal(kcal);
        produto.setProteinaG(proteinaG);
        produto.setChoG(choG);
        produto.setAcucarG(acucarG);
        produto.setLipG(lipG);
        produto.setSodioMg(sodioMg);
        produto.setPotassioMg(potassioMg);
        produto.setFosforoMg(fosforoMg);
        produto.setFerroMg(ferroMg);
        produto.setFibrasG(fibrasG);
        produto.setOsmolaridadeMosmL(osmolaridadeMosmL);
        produto.setPapelArtesanal(papelArtesanal);
        produto.setObservacao(textoOuNulo(observacao));

        // Derivado do tipo, nunca vindo do cliente: o banco impõe que concordem.
        produto.setModuloProteico(tipo == TipoProdutoNutricional.MODULO_PROTEICO);
    }

    private ProdutoNutricional buscarEditavel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return produtoNutricionalRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> produtoNutricionalRepository.findByIdVisivelPara(id, tenantId)
                        .map(global -> (RuntimeException) new BadRequestException(GLOBAL_NAO_EDITAVEL))
                        .orElseGet(() -> new NotFoundException("Produto não encontrado")));
    }

    private String nomeEnum(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
