package com.nutri.hospitalar.pediatria.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaCreateDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaResponseDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaSelectDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaUpdateDto;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import com.nutri.hospitalar.pediatria.mapper.FormulaLacteaMapper;
import com.nutri.hospitalar.pediatria.repository.FormulaLacteaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo de fórmulas lácteas.
 *
 * <p><b>Ler alcança as globais; escrever, não.</b> As 10 fórmulas do sistema
 * vêm da planilha e são produtos de mercado — a composição do NAN 2 é a mesma
 * em qualquer hospital. Todo tenant as enxerga; nenhum as altera.
 *
 * <p>A recusa é explícita ({@link BadRequestException}) e não um 404 seco: o
 * usuário precisa entender que a fórmula existe e é do sistema, não que ela
 * sumiu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormulaLacteaService {

    private static final String GLOBAL_NAO_EDITAVEL =
            "Esta é uma fórmula do sistema e não pode ser alterada. "
                    + "Cadastre uma fórmula própria se precisar de outra composição.";

    private final FormulaLacteaRepository formulaLacteaRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<FormulaLacteaResponseDto> getAll(Pageable pageable, String nome,
                                                 Boolean ativo, Boolean global) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaLacteaRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, textoOuNulo(nome), ativo, global)
                .map(FormulaLacteaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<FormulaLacteaSelectDto> select(String termo) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaLacteaRepository.findForSelect(tenantId, textoOuNulo(termo))
                .stream()
                .map(FormulaLacteaMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public FormulaLacteaResponseDto findById(UUID id) {
        return FormulaLacteaMapper.toResponse(buscarVisivel(id));
    }

    /** Leitura: enxerga a do tenant e a global. Usado também pelo cálculo. */
    @Transactional(readOnly = true)
    public FormulaLactea buscarVisivel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaLacteaRepository.findByIdVisivelPara(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fórmula láctea não encontrada"));
    }

    @Transactional
    public FormulaLacteaResponseDto create(FormulaLacteaCreateDto dto) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        String nome = dto.nome().trim();

        if (formulaLacteaRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, nome))
            throw new ConflictException("Já existe uma fórmula com esse nome");

        FormulaLactea formula = new FormulaLactea();
        formula.setTenant(securityUtils.getTenantReference());
        formula.setNome(nome);
        formula.setKcalPor100ml(dto.kcalPor100ml());
        formula.setProteinaPor100ml(dto.proteinaPor100ml());
        formula.setAtivo(dto.ativo() == null || dto.ativo());

        FormulaLactea salva = formulaLacteaRepository.save(formula);
        log.info("Fórmula láctea criada id={}", salva.getId());
        return FormulaLacteaMapper.toResponse(salva);
    }

    @Transactional
    public FormulaLacteaResponseDto update(UUID id, FormulaLacteaUpdateDto dto) {
        FormulaLactea formula = buscarEditavel(id);
        UUID tenantId = formula.getTenant().getId();
        String nome = dto.nome().trim();

        if (formulaLacteaRepository.existsByTenantIdAndNomeIgnoreCaseAndIdNot(tenantId, nome, id))
            throw new ConflictException("Já existe uma fórmula com esse nome");

        formula.setNome(nome);
        formula.setKcalPor100ml(dto.kcalPor100ml());
        formula.setProteinaPor100ml(dto.proteinaPor100ml());

        log.info("Fórmula láctea alterada id={}", id);
        return FormulaLacteaMapper.toResponse(formulaLacteaRepository.save(formula));
    }

    @Transactional
    public FormulaLacteaResponseDto alterarAtivo(UUID id, boolean ativo) {
        FormulaLactea formula = buscarEditavel(id);
        formula.setAtivo(ativo);

        log.info("Fórmula láctea {} alterada para ativo={}", id, ativo);
        return FormulaLacteaMapper.toResponse(formulaLacteaRepository.save(formula));
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * A fórmula que este tenant pode alterar.
     *
     * <p>Busca primeiro sem o {@code OR IS NULL}: se não achar, verifica se ela
     * existe como global só para poder recusar com a mensagem certa. Devolver
     * 404 aqui faria o usuário procurar um registro que está bem à sua frente
     * na lista.
     */
    private FormulaLactea buscarEditavel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return formulaLacteaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> formulaLacteaRepository.findByIdVisivelPara(id, tenantId)
                        .map(global -> (RuntimeException) new BadRequestException(GLOBAL_NAO_EDITAVEL))
                        .orElseGet(() -> new NotFoundException("Fórmula láctea não encontrada")));
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
