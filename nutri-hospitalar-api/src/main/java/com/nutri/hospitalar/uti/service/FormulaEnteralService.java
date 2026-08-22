package com.nutri.hospitalar.uti.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralCreateDto;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralResponseDto;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralSelectDto;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralUpdateDto;
import com.nutri.hospitalar.uti.entity.FormulaEnteral;
import com.nutri.hospitalar.uti.enums.CategoriaFormulaEnteral;
import com.nutri.hospitalar.uti.mapper.FormulaEnteralMapper;
import com.nutri.hospitalar.uti.repository.FormulaEnteralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.UUID;

/**
 * Catálogo de fórmulas enterais.
 *
 * <p><b>Ler alcança as globais; escrever, não.</b> As 53 fórmulas do sistema vêm
 * da planilha e são produtos de mercado. Todo tenant as enxerga; nenhum as
 * altera, e a recusa é um {@link BadRequestException} explicado — não um 404
 * seco que faria o usuário procurar um registro que está à sua frente na lista.
 *
 * <p>Além disso, aqui mora a única regra de negócio própria deste catálogo:
 * <b>o fechamento energético</b>. O banco já tem o {@code CHECK}, mas violá-lo
 * chegaria ao usuário como erro de constraint. Conferimos antes para dizer o
 * que está errado e por quanto — foi este teste que encontrou os quatro produtos
 * errados da planilha de origem.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormulaEnteralService {

    private static final String GLOBAL_NAO_EDITAVEL =
            "Esta é uma fórmula do sistema e não pode ser alterada. "
                    + "Cadastre uma fórmula própria se precisar de outra composição.";

    /**
     * Tolerância do fechamento energético — a mesma do {@code CHECK} da
     * migration {@code 018}.
     *
     * <p>12 % não é generosidade: rótulo de fórmula enteral arredonda macro, e o
     * fator de Atwater é aproximação. O que a faixa pega são os erros de ordem
     * de grandeza — densidade trocada, fator 10 no lipídio, composição por
     * embalagem em vez de por litro.
     */
    private static final BigDecimal TOLERANCIA_FECHAMENTO = new BigDecimal("0.12");

    private static final BigDecimal KCAL_POR_G_PROTEINA = new BigDecimal("4");
    private static final BigDecimal KCAL_POR_G_CHO      = new BigDecimal("4");
    private static final BigDecimal KCAL_POR_G_LIPIDIO  = new BigDecimal("9");
    private static final BigDecimal ML_POR_LITRO        = new BigDecimal("1000");

    private final FormulaEnteralRepository formulaEnteralRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<FormulaEnteralResponseDto> getAll(Pageable pageable, String nome,
                                                  CategoriaFormulaEnteral categoria,
                                                  Boolean ativo, Boolean global) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaEnteralRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, textoOuNulo(nome), nomeEnum(categoria), ativo, global)
                .map(FormulaEnteralMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<FormulaEnteralSelectDto> select(String termo) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaEnteralRepository.findForSelect(tenantId, textoOuNulo(termo))
                .stream()
                .map(FormulaEnteralMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public FormulaEnteralResponseDto findById(UUID id) {
        return FormulaEnteralMapper.toResponse(buscarVisivel(id));
    }

    /** Leitura: enxerga a do tenant e a global. Usado também pelo cálculo. */
    @Transactional(readOnly = true)
    public FormulaEnteral buscarVisivel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaEnteralRepository.findByIdVisivelPara(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fórmula enteral não encontrada"));
    }

    @Transactional
    public FormulaEnteralResponseDto create(FormulaEnteralCreateDto dto) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        String nome = dto.nome().trim();

        if (formulaEnteralRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, nome))
            throw new ConflictException("Já existe uma fórmula com esse nome");

        validarFechamentoEnergetico(dto.densidadeKcalMl(), dto.proteinaGL(),
                dto.choGL(), dto.lipGL());

        FormulaEnteral formula = new FormulaEnteral();
        formula.setTenant(securityUtils.getTenantReference());
        formula.setNome(nome);
        aplicar(formula, dto.categoria(), dto.densidadeKcalMl(), dto.proteinaGL(),
                dto.choGL(), dto.lipGL(), dto.fibrasGL(), dto.potassioMgL(),
                dto.osmolaridadeMosmL(), dto.aguaLivrePerc());
        formula.setAtivo(dto.ativo() == null || dto.ativo());

        FormulaEnteral salva = formulaEnteralRepository.save(formula);
        log.info("Fórmula enteral criada id={}", salva.getId());
        return FormulaEnteralMapper.toResponse(salva);
    }

    @Transactional
    public FormulaEnteralResponseDto update(UUID id, FormulaEnteralUpdateDto dto) {
        FormulaEnteral formula = buscarEditavel(id);
        UUID tenantId = formula.getTenant().getId();
        String nome = dto.nome().trim();

        if (formulaEnteralRepository.existsByTenantIdAndNomeIgnoreCaseAndIdNot(tenantId, nome, id))
            throw new ConflictException("Já existe uma fórmula com esse nome");

        validarFechamentoEnergetico(dto.densidadeKcalMl(), dto.proteinaGL(),
                dto.choGL(), dto.lipGL());

        formula.setNome(nome);
        aplicar(formula, dto.categoria(), dto.densidadeKcalMl(), dto.proteinaGL(),
                dto.choGL(), dto.lipGL(), dto.fibrasGL(), dto.potassioMgL(),
                dto.osmolaridadeMosmL(), dto.aguaLivrePerc());

        log.info("Fórmula enteral alterada id={}", id);
        return FormulaEnteralMapper.toResponse(formulaEnteralRepository.save(formula));
    }

    @Transactional
    public FormulaEnteralResponseDto alterarAtivo(UUID id, boolean ativo) {
        FormulaEnteral formula = buscarEditavel(id);
        formula.setAtivo(ativo);

        log.info("Fórmula enteral {} alterada para ativo={}", id, ativo);
        return FormulaEnteralMapper.toResponse(formulaEnteralRepository.save(formula));
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * A soma dos macros por Atwater tem de bater com a densidade declarada.
     *
     * <p>Só confere quando carboidrato e lipídio estão preenchidos: sem os três
     * macros não há o que fechar, e exigi-los transformaria uma consulta de
     * rótulo incompleto em cadastro impossível.
     *
     * <p>É esta conta que denuncia o erro que mais importa — composição por
     * embalagem de 500 ml lançada como se fosse por litro fecha em −50 %.
     */
    private void validarFechamentoEnergetico(BigDecimal densidadeKcalMl,
                                             BigDecimal proteinaGL,
                                             BigDecimal choGL,
                                             BigDecimal lipGL) {
        if (choGL == null || lipGL == null) return;

        BigDecimal kcalDeclarada = densidadeKcalMl.multiply(ML_POR_LITRO, MathContext.DECIMAL64);
        if (kcalDeclarada.signum() == 0) return;

        BigDecimal kcalDosMacros = proteinaGL.multiply(KCAL_POR_G_PROTEINA, MathContext.DECIMAL64)
                .add(choGL.multiply(KCAL_POR_G_CHO, MathContext.DECIMAL64), MathContext.DECIMAL64)
                .add(lipGL.multiply(KCAL_POR_G_LIPIDIO, MathContext.DECIMAL64), MathContext.DECIMAL64);

        BigDecimal desvio = kcalDosMacros.subtract(kcalDeclarada, MathContext.DECIMAL64)
                .divide(kcalDeclarada, MathContext.DECIMAL64);

        if (desvio.abs().compareTo(TOLERANCIA_FECHAMENTO) > 0)
            throw new BadRequestException(mensagemDeFechamento(desvio, kcalDeclarada, kcalDosMacros));
    }

    /**
     * A mensagem diz o desvio e nomeia a causa provável.
     *
     * <p>"Não fecha" sozinho manda o usuário conferir sete campos. As duas
     * causas de longe mais comuns têm assinatura numérica: composição por
     * embalagem de 500 ml lançada como litro fecha perto de −50 %, e um fator 10
     * num macro estoura para cima.
     */
    private String mensagemDeFechamento(BigDecimal desvio, BigDecimal kcalDeclarada,
                                        BigDecimal kcalDosMacros) {
        String base = ("A composição não fecha com a densidade: os macros somam %.0f kcal/L "
                + "por Atwater (4·PTN + 4·CHO + 9·LIP) e a densidade declara %.0f kcal/L — "
                + "desvio de %+.1f %%.")
                .formatted(kcalDosMacros, kcalDeclarada,
                        desvio.multiply(new BigDecimal("100"), MathContext.DECIMAL64));

        if (desvio.signum() < 0)
            return base + " Confira se a composição está por LITRO: produto em frasco de "
                    + "500 ml lançado como se fosse litro fecha perto de −50 %.";

        return base + " Confira se algum macro está com o ponto decimal deslocado.";
    }

    private void aplicar(FormulaEnteral formula, CategoriaFormulaEnteral categoria,
                         BigDecimal densidadeKcalMl, BigDecimal proteinaGL,
                         BigDecimal choGL, BigDecimal lipGL, BigDecimal fibrasGL,
                         BigDecimal potassioMgL, BigDecimal osmolaridadeMosmL,
                         BigDecimal aguaLivrePerc) {
        formula.setCategoria(categoria);
        formula.setDensidadeKcalMl(densidadeKcalMl);
        formula.setProteinaGL(proteinaGL);
        formula.setChoGL(choGL);
        formula.setLipGL(lipGL);
        formula.setFibrasGL(fibrasGL);
        formula.setPotassioMgL(potassioMgL);
        formula.setOsmolaridadeMosmL(osmolaridadeMosmL);
        formula.setAguaLivrePerc(aguaLivrePerc);
    }

    /**
     * A fórmula que este tenant pode alterar.
     *
     * <p>Busca primeiro sem o {@code OR IS NULL}: se não achar, verifica se ela
     * existe como global só para poder recusar com a mensagem certa.
     */
    private FormulaEnteral buscarEditavel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return formulaEnteralRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> formulaEnteralRepository.findByIdVisivelPara(id, tenantId)
                        .map(global -> (RuntimeException) new BadRequestException(GLOBAL_NAO_EDITAVEL))
                        .orElseGet(() -> new NotFoundException("Fórmula enteral não encontrada")));
    }

    private String nomeEnum(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
