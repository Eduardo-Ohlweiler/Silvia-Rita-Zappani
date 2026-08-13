package com.nutri.hospitalar.tenant.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.tenant.dtos.TenantResponseDto;
import com.nutri.hospitalar.tenant.dtos.TenantSelectDto;
import com.nutri.hospitalar.tenant.dtos.TenantUpdateDto;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.enums.PeriodoAcesso;
import com.nutri.hospitalar.tenant.mapper.TenantMapper;
import com.nutri.hospitalar.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;

    @Value("${app.acesso.fuso-horario:America/Sao_Paulo}")
    private String fusoHorario;

    @Transactional(readOnly = true)
    public Page<TenantResponseDto> getAll(Pageable pageable, String nome, Boolean ativo,
                                          Integer expirandoEmDias) {
        return tenantRepository
                .findAllWithFilters(PageableUtils.semOrdenacao(pageable), nome, ativo, expirandoEmDias)
                .map(TenantMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TenantSelectDto> select(String nome) {
        return tenantRepository.findForSelect(nome).stream()
                .map(TenantMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponseDto findByIdResponse(UUID id) {
        return TenantMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Tenant findById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant não encontrado"));
    }

    @Transactional
    public TenantResponseDto update(UUID id, TenantUpdateDto dto) {
        Tenant tenant = findById(id);
        tenant.setNome(dto.nome());
        return TenantMapper.toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponseDto alterarAtivo(UUID id, boolean ativo) {
        Tenant tenant = findById(id);

        // Reativar sem renovar seria desfeito pela rotina na madrugada seguinte:
        // o superadmin acharia que resolveu e o cliente perderia o acesso outra vez.
        if (ativo && tenant.acessoExpirado())
            throw new ConflictException("Renove o período de acesso para reativar este cliente");

        tenant.setAtivo(ativo);
        log.info("Tenant {} alterado para ativo={}", id, ativo);
        return TenantMapper.toResponse(tenantRepository.save(tenant));
    }

    /**
     * Define — ou renova — o período de acesso, sempre recontado a partir de
     * hoje.
     *
     * <p>Reativa junto quando o cliente estava desligado com o prazo vencido:
     * é exatamente a operação "pagou, volta a ter acesso". Um tenant desativado
     * à mão, sem prazo vencido, continua desativado — desligar foi decisão de
     * alguém, e renovar contrato não a desfaz.
     */
    @Transactional
    public TenantResponseDto definirAcesso(UUID id, PeriodoAcesso periodo) {
        Tenant tenant = findById(id);
        boolean reativando = !Boolean.TRUE.equals(tenant.getAtivo()) && tenant.acessoExpirado();

        aplicarPeriodo(tenant, periodo);

        if (reativando) tenant.setAtivo(true);

        log.info("Tenant {} com período de acesso {} até {}{}",
                id, periodo, tenant.getAcessoExpiraEm(), reativando ? " (reativado)" : "");

        return TenantMapper.toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public Tenant criar(String nome, PeriodoAcesso periodo) {
        Tenant tenant = new Tenant(nome);
        aplicarPeriodo(tenant, periodo != null ? periodo : PeriodoAcesso.INDETERMINADO);
        return tenantRepository.save(tenant);
    }

    /** Chamado pela rotina diária. Devolve quantos clientes foram desligados. */
    @Transactional
    public int inativarExpirados(Instant agora) {
        return tenantRepository.inativarExpirados(agora);
    }

    private void aplicarPeriodo(Tenant tenant, PeriodoAcesso periodo) {
        ZoneId zona = ZoneId.of(fusoHorario);
        tenant.setPeriodoAcesso(periodo);
        tenant.setAcessoExpiraEm(periodo.expiracaoAPartirDe(LocalDate.now(zona), zona));
    }
}
