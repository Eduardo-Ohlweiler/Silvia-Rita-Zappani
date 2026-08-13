package com.nutri.hospitalar.usuario.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.ForbiddenException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.service.TenantService;
import com.nutri.hospitalar.usuario.dtos.PerfilUpdateDto;
import com.nutri.hospitalar.usuario.dtos.SenhaUpdateDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioCreateDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioResponseDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioSelectDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioUpdateDto;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.enums.Role;
import com.nutri.hospitalar.usuario.mapper.UsuarioMapper;
import com.nutri.hospitalar.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    @Value("${app.seguranca.max-tentativas-login:5}")
    private int maxTentativasLogin;

    @Value("${app.seguranca.duracao-bloqueio:PT15M}")
    private Duration duracaoBloqueio;

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> getAll(Pageable pageable, String nome, String email,
                                           Role role, Boolean ativo) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return usuarioRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, nome, email,
                        role != null ? role.name() : null,
                        ativo)
                .map(UsuarioMapper::toResponse);
    }

    /**
     * Listagem através dos tenants — exclusiva de SUPERADMIN.
     *
     * <p>É o único ponto do módulo que lê fora do tenant efetivo, e existe por
     * uma razão concreta: o superadmin cadastra um cliente novo e o usuário
     * nasce no tenant recém-criado, não no tenant em que ele está.
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> getAllGlobal(Pageable pageable, UUID tenantId, String nome,
                                                 String email, Role role, Boolean ativo) {
        return usuarioRepository.findAllGlobal(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, nome, email,
                        role != null ? role.name() : null,
                        ativo)
                .map(UsuarioMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UsuarioSelectDto> select(String nome) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return usuarioRepository.findForSelect(tenantId, nome).stream()
                .map(UsuarioMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto findByIdResponse(UUID id) {
        return UsuarioMapper.toResponse(findByIdNoTenant(id));
    }

    @Transactional(readOnly = true)
    public Usuario findByIdNoTenant(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return usuarioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    @Transactional
    public UsuarioResponseDto create(UsuarioCreateDto dto) {
        String email = normalizar(dto.email());

        if (usuarioRepository.existsByEmailIgnoreCase(email))
            throw new ConflictException("Já existe um usuário com esse e-mail");

        boolean tenantNovo = ehClienteNovo(dto);
        Tenant tenant = tenantNovo
                ? tenantService.criar(dto.nome(), dto.periodoAcesso())
                : resolverTenantExistente(dto.tenantId());

        if (!Boolean.TRUE.equals(tenant.getAtivo()))
            throw new ConflictException("Tenant inativo");

        Role role = tenantNovo ? Role.ADMIN : exigirRole(dto.role());
        validarConcessaoDeRole(role);

        Usuario usuario = new Usuario();
        usuario.setTenant(tenant);
        usuario.setNome(dto.nome());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setTelefone(dto.telefone());
        usuario.setCodigoPais(codigoPaisOuPadrao(dto.codigoPais()));
        usuario.setRole(role);
        usuario.setCreatedBy(securityUtils.getUsuarioLogado());

        Usuario salvo = usuarioRepository.save(usuario);

        if (tenantNovo)
            log.info("Cliente novo: tenant {} criado com o admin {}", tenant.getId(), salvo.getId());
        else
            log.info("Usuário criado id={} tenantId={} role={}",
                    salvo.getId(), tenant.getId(), salvo.getRole());

        return UsuarioMapper.toResponse(salvo);
    }

    @Transactional
    public UsuarioResponseDto update(UUID id, UsuarioUpdateDto dto) {
        Usuario usuario = findByIdNoTenant(id);
        String email = normalizar(dto.email());

        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id))
            throw new ConflictException("Já existe um usuário com esse e-mail");

        if (!usuario.getRole().equals(dto.role())) {
            validarConcessaoDeRole(dto.role());
            impedirRebaixamentoDoProprioAcesso(usuario, dto.role());
            log.info("Role do usuário {} alterada de {} para {}",
                    id, usuario.getRole(), dto.role());
        }

        usuario.setNome(dto.nome());
        usuario.setEmail(email);
        usuario.setTelefone(dto.telefone());
        usuario.setCodigoPais(codigoPaisOuPadrao(dto.codigoPais()));
        usuario.setRole(dto.role());
        usuario.setUpdatedBy(securityUtils.getUsuarioLogado());

        return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDto alterarAtivo(UUID id, boolean ativo) {
        Usuario usuario = findByIdNoTenant(id);

        if (!ativo && usuario.getId().equals(securityUtils.getUsuarioIdLogado()))
            throw new BadRequestException("Você não pode desativar o próprio usuário");

        usuario.setAtivo(ativo);
        usuario.setUpdatedBy(securityUtils.getUsuarioLogado());

        if (ativo) usuario.registrarLoginValido();      // libera bloqueio por tentativas

        log.info("Usuário {} alterado para ativo={}", id, ativo);
        return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDto desbloquear(UUID id) {
        Usuario usuario = findByIdNoTenant(id);
        usuario.registrarLoginValido();
        usuario.setUpdatedBy(securityUtils.getUsuarioLogado());
        log.info("Usuário {} desbloqueado", id);
        return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarTentativaInvalida(UUID usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            int tentativas = usuario.getTentativasFalhas() + 1;
            usuario.setTentativasFalhas(tentativas);

            if (tentativas >= maxTentativasLogin) {
                usuario.setBloqueadoAte(Instant.now().plus(duracaoBloqueio));
                log.warn("Usuário {} bloqueado após {} tentativas", usuarioId, tentativas);
            }
            usuarioRepository.save(usuario);
        });
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto getPerfil() {
        return UsuarioMapper.toResponse(securityUtils.getUsuarioLogado());
    }

    @Transactional
    public UsuarioResponseDto updatePerfil(PerfilUpdateDto dto) {
        Usuario usuario = securityUtils.getUsuarioLogado();
        usuario.setNome(dto.nome());
        usuario.setTelefone(dto.telefone());
        usuario.setCodigoPais(codigoPaisOuPadrao(dto.codigoPais()));
        return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void alterarSenha(SenhaUpdateDto dto) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha()))
            throw new BadRequestException("Senha atual incorreta");

        if (passwordEncoder.matches(dto.senhaNova(), usuario.getSenha()))
            throw new BadRequestException("A nova senha deve ser diferente da atual");

        usuario.setSenha(passwordEncoder.encode(dto.senhaNova()));
        usuarioRepository.save(usuario);
        log.info("Senha alterada para o usuário {}", usuario.getId());
    }

    private boolean ehClienteNovo(UsuarioCreateDto dto) {
        return dto.tenantId() == null && !securityUtils.isImpersonating();
    }

    private Tenant resolverTenantExistente(UUID tenantIdInformado) {
        return tenantIdInformado != null
                ? tenantService.findById(tenantIdInformado)
                : securityUtils.getTenantReference();
    }

    private Role exigirRole(Role role) {
        if (role == null)
            throw new BadRequestException("Informe o nível de acesso do usuário");
        return role;
    }

    private void validarConcessaoDeRole(Role role) {
        if (Role.SUPERADMIN.equals(role) && !securityUtils.isSuperadmin())
            throw new ForbiddenException("Operação não permitida");
    }

    private void impedirRebaixamentoDoProprioAcesso(Usuario alvo, Role novaRole) {
        boolean ehEuMesmo = alvo.getId().equals(securityUtils.getUsuarioIdLogado());
        boolean estaPerdendoAcesso = !Role.SUPERADMIN.equals(novaRole)
                && Role.SUPERADMIN.equals(alvo.getRole());
        if (ehEuMesmo && estaPerdendoAcesso)
            throw new BadRequestException("Você não pode rebaixar o próprio nível de acesso");
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String codigoPaisOuPadrao(String codigoPais) {
        return (codigoPais == null || codigoPais.isBlank()) ? "55" : codigoPais;
    }
}
