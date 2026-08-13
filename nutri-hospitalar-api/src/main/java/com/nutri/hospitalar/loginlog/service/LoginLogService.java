package com.nutri.hospitalar.loginlog.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.loginlog.dtos.LoginLogResponseDto;
import com.nutri.hospitalar.loginlog.entity.LoginLog;
import com.nutri.hospitalar.loginlog.enums.MotivoFalha;
import com.nutri.hospitalar.loginlog.enums.TipoLogout;
import com.nutri.hospitalar.loginlog.mapper.LoginLogMapper;
import com.nutri.hospitalar.loginlog.repository.LoginLogRepository;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.usuario.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLogService {

    private static final int TAMANHO_MAX_USER_AGENT = 512;

    private final LoginLogRepository loginLogRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public UUID abrirSessao(Usuario usuario, HttpServletRequest request, Usuario impersonadoPor) {
        LoginLog registro = base(request);
        registro.setTenant(usuario.getTenant());
        registro.setUsuario(usuario);
        registro.setEmailTentativa(usuario.getEmail());
        registro.setSucesso(true);
        registro.setImpersonadoPor(impersonadoPor);
        return loginLogRepository.save(registro).getId();
    }

    @Transactional
    public UUID abrirSessaoImpersonada(Usuario superadmin, Tenant destino, HttpServletRequest request) {
        LoginLog registro = base(request);
        registro.setTenant(destino);
        registro.setUsuario(superadmin);
        registro.setEmailTentativa(superadmin.getEmail());
        registro.setSucesso(true);
        registro.setImpersonadoPor(superadmin);
        log.info("Superadmin {} entrou no tenant {}", superadmin.getId(), destino.getId());
        return loginLogRepository.save(registro).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(Usuario usuario, String emailTentativa,
                               MotivoFalha motivo, HttpServletRequest request) {
        LoginLog registro = base(request);
        registro.setUsuario(usuario);
        registro.setTenant(usuario != null ? usuario.getTenant() : null);
        registro.setEmailTentativa(emailTentativa);
        registro.setSucesso(false);
        registro.setMotivoFalha(motivo);
        loginLogRepository.save(registro);

        log.warn("Falha de login motivo={} ip={}", motivo, registro.getEnderecoIp());
    }

    @Transactional
    public void fecharSessao(UUID sessionId, TipoLogout tipo) {
        if (sessionId == null) return;

        loginLogRepository.findById(sessionId).ifPresent(registro -> {
            if (registro.getDataLogout() == null) {
                registro.setDataLogout(Instant.now());
                registro.setTipoLogout(tipo);
                loginLogRepository.save(registro);
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<LoginLogResponseDto> getAll(Pageable pageable, UUID usuarioId, Boolean sucesso,
                                            Instant de, Instant ate, String ip) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return loginLogRepository
                .findAllWithFilters(PageableUtils.semOrdenacao(pageable),
                        tenantId, usuarioId, sucesso, de, ate, ip)
                .map(LoginLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LoginLogResponseDto> getAllGlobal(Pageable pageable, UUID tenantId, UUID usuarioId,
                                                  Boolean sucesso, Instant de, Instant ate, String ip) {
        return loginLogRepository
                .findAllGlobal(PageableUtils.semOrdenacao(pageable),
                        tenantId, usuarioId, sucesso, de, ate, ip)
                .map(LoginLogMapper::toResponse);
    }

    @Transactional
    public int fecharSessoesExpiradas(Instant limite) {
        return loginLogRepository.fecharSessoesExpiradas(limite, Instant.now());
    }

    @Transactional
    public int expurgarAnterioresA(Instant limite) {
        return loginLogRepository.expurgarAnterioresA(limite);
    }

    private LoginLog base(HttpServletRequest request) {
        LoginLog registro = new LoginLog();
        registro.setDataLogin(Instant.now());
        registro.setEnderecoIp(ipDe(request));
        registro.setUserAgent(truncar(request != null ? request.getHeader("User-Agent") : null));
        return registro;
    }

    private String ipDe(HttpServletRequest request) {
        if (request == null) return null;
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank())
            return encaminhado.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String truncar(String valor) {
        if (valor == null) return null;
        return valor.length() <= TAMANHO_MAX_USER_AGENT
                ? valor
                : valor.substring(0, TAMANHO_MAX_USER_AGENT);
    }
}
