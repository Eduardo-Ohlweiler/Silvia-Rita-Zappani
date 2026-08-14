package com.nutri.hospitalar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutri.hospitalar.loginlog.repository.LoginLogRepository;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import com.nutri.hospitalar.refreshtoken.repository.RefreshTokenRepository;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.repository.TenantRepository;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.enums.Role;
import com.nutri.hospitalar.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração.
 *
 * <p>Roda contra um PostgreSQL local dedicado ({@code nutridb_test}), não H2 e
 * não Testcontainers — o projeto não usa Docker, e H2 não tem {@code unaccent}
 * nem o mesmo comportamento de {@code NULL} em constraint única, o que daria
 * falsa confiança exatamente onde dói.
 *
 * <p>Monta o cenário canônico de isolamento: dois tenants completos, cada um
 * com o seu administrador, mais o superadmin do tenant raiz.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String SENHA_PADRAO = "SenhaDeTeste123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected TenantRepository tenantRepository;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected LoginLogRepository loginLogRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;
    @Autowired protected PessoaRepository pessoaRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected Tenant tenantA;
    protected Tenant tenantB;
    protected Usuario adminA;
    protected Usuario adminB;
    protected Usuario userA;
    protected Usuario superadmin;

    @BeforeEach
    void montarCenario() {
        // Ordem de dependência:
        //   refresh_token → login_log → pessoa → usuario → tenant
        // Apagar a pessoa leva junto telefone, e-mail, rede social e os tipos
        // de cadastro: as FKs são ON DELETE CASCADE (migration 010).
        refreshTokenRepository.deleteAllInBatch();
        loginLogRepository.deleteAllInBatch();
        pessoaRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();

        tenantA = tenantRepository.save(new Tenant("Clinica A"));
        tenantB = tenantRepository.save(new Tenant("Hospital B"));
        Tenant raiz = tenantRepository.save(new Tenant("Nutri Hospitalar Admin"));

        adminA = criarUsuario(tenantA, "admin.a@teste.local", Role.ADMIN);
        userA = criarUsuario(tenantA, "user.a@teste.local", Role.USER);
        adminB = criarUsuario(tenantB, "admin.b@teste.local", Role.ADMIN);
        superadmin = criarUsuario(raiz, "root@teste.local", Role.SUPERADMIN);
    }

    protected Usuario criarUsuario(Tenant tenant, String email, Role role) {
        Usuario usuario = new Usuario();
        usuario.setTenant(tenant);
        usuario.setNome("Usuario " + email);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA_PADRAO));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    /** Autentica de verdade, pelo endpoint — o token sai igual ao de produção. */
    protected String autenticar(String email) throws Exception {
        return autenticar(email, SENHA_PADRAO);
    }

    protected String autenticar(String email, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + objectMapper.readTree(corpo).get("accessToken").asText();
    }
}
