package com.nutri.hospitalar.auth.service;

import com.nutri.hospitalar.auth.dtos.AuthResponseDto;
import com.nutri.hospitalar.auth.dtos.LoginDto;
import com.nutri.hospitalar.config.JwtUtil;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.ForbiddenException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.exceptions.UnauthorizedException;
import com.nutri.hospitalar.loginlog.enums.MotivoFalha;
import com.nutri.hospitalar.loginlog.enums.TipoLogout;
import com.nutri.hospitalar.loginlog.service.LoginLogService;
import com.nutri.hospitalar.refreshtoken.entity.RefreshToken;
import com.nutri.hospitalar.refreshtoken.service.RefreshTokenService;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.repository.TenantRepository;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.repository.UsuarioRepository;
import com.nutri.hospitalar.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";
    private static final String ACESSO_ENCERRADO =
            "Período de acesso encerrado. Contate o administrador.";

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final UsuarioService usuarioService;
    private final LoginLogService loginLogService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecurityUtils securityUtils;

    @Transactional
    public AuthResponseDto login(LoginDto dto, HttpServletRequest request) {
        String email = dto.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);

        if (usuario == null) {
            loginLogService.registrarFalha(null, email, MotivoFalha.USUARIO_INEXISTENTE, request);
            throw new UnauthorizedException(CREDENCIAIS_INVALIDAS);
        }

        if (usuario.estaBloqueado()) {
            loginLogService.registrarFalha(usuario, email, MotivoFalha.BLOQUEADO, request);
            throw new UnauthorizedException(
                    "Conta temporariamente bloqueada por tentativas inválidas. Tente mais tarde.");
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            loginLogService.registrarFalha(usuario, email, MotivoFalha.USUARIO_INATIVO, request);
            throw new UnauthorizedException("Usuário inativo. Contate o administrador.");
        }

        Tenant tenant = usuario.getTenant();
        if (!Boolean.TRUE.equals(tenant.getAtivo())) {
            loginLogService.registrarFalha(usuario, email, MotivoFalha.TENANT_INATIVO, request);
            throw new UnauthorizedException("Acesso bloqueado. Contate o administrador.");
        }

        // Não espera a rotina diária: cobre a janela entre a virada do dia e o
        // job, e cobre o servidor ter ficado fora do ar na hora dele.
        if (acessoVencido(usuario, tenant)) {
            loginLogService.registrarFalha(usuario, email, MotivoFalha.ACESSO_EXPIRADO, request);
            throw new UnauthorizedException(ACESSO_ENCERRADO);
        }

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            usuarioService.registrarTentativaInvalida(usuario.getId());   // REQUIRES_NEW
            loginLogService.registrarFalha(usuario, email, MotivoFalha.SENHA_INVALIDA, request);
            throw new UnauthorizedException(CREDENCIAIS_INVALIDAS);
        }

        usuario.registrarLoginValido();
        usuario = usuarioRepository.save(usuario);

        return emitirSessao(usuario, tenant, request, null);
    }

    @Transactional
    public void logout() {
        loginLogService.fecharSessao(securityUtils.getSessionIdLogado(), TipoLogout.MANUAL);
        refreshTokenService.revogarTodosDoUsuario(securityUtils.getUsuarioIdLogado());
    }

    @Transactional
    public AuthResponseDto refresh(String refreshToken) {
        RefreshToken consumido = refreshTokenService.consumir(refreshToken);
        Usuario usuario = consumido.getUsuario();

        if (!Boolean.TRUE.equals(usuario.getAtivo()))
            throw new UnauthorizedException("Usuário inativo. Contate o administrador.");

        Tenant tenant = usuario.getTenant();
        if (!Boolean.TRUE.equals(tenant.getAtivo()))
            throw new UnauthorizedException("Acesso bloqueado. Contate o administrador.");

        // Mesma porta que o login: sem esta linha, um cliente vencido seguiria
        // renovando a sessão por até o tempo de vida do refresh token.
        if (acessoVencido(usuario, tenant))
            throw new UnauthorizedException(ACESSO_ENCERRADO);

        UUID sessionId = consumido.getSessionId();

        String access = jwtUtil.gerarAccessToken(
                usuario.getId(), tenant.getId(), usuario.getRole(), sessionId, false);
        String novoRefresh = refreshTokenService.emitir(usuario, sessionId);

        return montarResposta(access, novoRefresh, usuario, tenant, false);
    }

    @Transactional
    public AuthResponseDto switchTenant(UUID tenantId, HttpServletRequest request) {
        Usuario superadmin = exigirSuperadmin();

        Tenant destino = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant não encontrado"));

        if (!Boolean.TRUE.equals(destino.getAtivo()))
            throw new ConflictException("Tenant inativo");

        loginLogService.fecharSessao(securityUtils.getSessionIdLogado(), TipoLogout.MANUAL);
        UUID sessionId = loginLogService.abrirSessaoImpersonada(superadmin, destino, request);

        String access = jwtUtil.gerarAccessToken(
                superadmin.getId(), destino.getId(), superadmin.getRole(), sessionId, true);

        return montarResposta(access, null, superadmin, destino, true);
    }

    @Transactional
    public AuthResponseDto exitTenant(HttpServletRequest request) {
        Usuario superadmin = exigirSuperadmin();
        Tenant origem = superadmin.getTenant();

        loginLogService.fecharSessao(securityUtils.getSessionIdLogado(), TipoLogout.MANUAL);
        UUID sessionId = loginLogService.abrirSessao(superadmin, request, null);

        String access = jwtUtil.gerarAccessToken(
                superadmin.getId(), origem.getId(), superadmin.getRole(), sessionId, false);

        return montarResposta(access, null, superadmin, origem, false);
    }

    /**
     * O superadmin é poupado pelo mesmo motivo da trava da rotina diária: uma
     * data gravada errado no tenant raiz trancaria todo mundo para fora.
     */
    private boolean acessoVencido(Usuario usuario, Tenant tenant) {
        return !usuario.isSuperadmin() && tenant.acessoExpirado();
    }

    private Usuario exigirSuperadmin() {
        Usuario usuario = securityUtils.getUsuarioLogado();
        if (!usuario.isSuperadmin())
            throw new ForbiddenException("Operação não permitida");
        return usuario;
    }

    private AuthResponseDto emitirSessao(Usuario usuario, Tenant tenant,
                                         HttpServletRequest request, Usuario impersonadoPor) {
        UUID sessionId = loginLogService.abrirSessao(usuario, request, impersonadoPor);

        String access = jwtUtil.gerarAccessToken(
                usuario.getId(), tenant.getId(), usuario.getRole(), sessionId, false);
        String refresh = refreshTokenService.emitir(usuario, sessionId);

        return montarResposta(access, refresh, usuario, tenant, false);
    }

    private AuthResponseDto montarResposta(String access, String refresh, Usuario usuario,
                                           Tenant tenant, boolean impersonating) {
        return new AuthResponseDto(
                access,
                refresh,
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                tenant.getId(),
                tenant.getNome(),
                impersonating);
    }
}
