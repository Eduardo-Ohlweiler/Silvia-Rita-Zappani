package com.nutri.hospitalar.usuario;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um cliente novo nasce ao cadastrar o seu primeiro usuário — não há tela de
 * cadastro de tenant. O destino do usuário depende de {@code tenantId} e do
 * tenant efetivo de quem cadastra.
 */
@DisplayName("Criação de cliente e de usuário")
class CriacaoDeClienteTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("no tenant raiz e sem tenantId: cria o tenant e o usuário nasce ADMIN dele")
    void semTenantIdNoRaizCriaClienteNovo() throws Exception {
        long tenantsAntes = tenantRepository.count();

        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Carla Nutricionista","email":"carla@nova.local",
                                 "senha":"SenhaDaCarla12"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.tenantNome").value("Carla Nutricionista"));

        assertThat(tenantRepository.count()).isEqualTo(tenantsAntes + 1);

        Usuario criada = usuarioRepository.findByEmailIgnoreCase("carla@nova.local").orElseThrow();
        assertThat(criada.getRole()).isEqualTo(Role.ADMIN);
        assertThat(criada.getTenant().getId()).isNotEqualTo(superadmin.getTenant().getId());
    }

    @Test
    @DisplayName("a role informada é ignorada ao abrir um cliente novo — o tenant não pode ficar sem admin")
    void tenantNovoIgnoraRoleInformada() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Bruno","email":"bruno@novo.local",
                                 "senha":"SenhaDoBruno12","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("com tenantId: o usuário entra no tenant existente e nenhum tenant é criado")
    void comTenantIdEntraNoTenantExistente() throws Exception {
        long tenantsAntes = tenantRepository.count();

        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"tenantId":"%s","nome":"Ana","email":"ana@clinicaA.local",
                                 "senha":"SenhaDaAna123","role":"USER"}
                                """.formatted(tenantA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantA.getId().toString()))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(tenantRepository.count()).isEqualTo(tenantsAntes);
    }

    @Test
    @DisplayName("dentro de um tenant: o usuário entra nele, sem criar tenant novo")
    void dentroDeUmTenantAcrescentaAEquipe() throws Exception {
        long tenantsAntes = tenantRepository.count();
        String noTenantA = tokenNoTenantA();

        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, noTenantA)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Paula","email":"paula@clinicaA.local",
                                 "senha":"SenhaDaPaula12","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantA.getId().toString()))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(tenantRepository.count()).isEqualTo(tenantsAntes);
    }

    @Test
    @DisplayName("o usuário de um cliente novo NÃO aparece na listagem do tenant efetivo, mas aparece na global")
    void clienteNovoSoApareceNaListagemGlobal() throws Exception {
        String token = autenticar(superadmin.getEmail());

        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fernanda Mattos","email":"fernanda@nova.local",
                                 "senha":"SenhaFernanda12"}
                                """))
                .andExpect(status().isCreated());

        // A listagem por tenant mostra o tenant EFETIVO — e o superadmin está
        // no raiz, não no tenant que acabou de nascer.
        mockMvc.perform(get("/usuarios").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email",
                        not(hasItem("fernanda@nova.local"))));

        // A global encontra, e é para isso que ela existe.
        mockMvc.perform(get("/usuarios/global")
                        .param("nome", "Fernanda")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("fernanda@nova.local"))
                .andExpect(jsonPath("$.content[0].tenantNome").value("Fernanda Mattos"));
    }

    @Test
    @DisplayName("a listagem global filtra por tenant quando pedido")
    void globalFiltraPorTenant() throws Exception {
        mockMvc.perform(get("/usuarios/global")
                        .param("tenantId", tenantA.getId().toString())
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))   // adminA e userA
                .andExpect(jsonPath("$.content[*].tenantId",
                        everyItem(is(tenantA.getId().toString()))));
    }

    @Test
    @DisplayName("ADMIN e USER não alcançam a listagem global")
    void globalEExclusivaDoSuperadmin() throws Exception {
        for (String email : new String[]{adminA.getEmail(), userA.getEmail()}) {
            mockMvc.perform(get("/usuarios/global").header(AUTHORIZATION, autenticar(email)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("senha com menos de 10 caracteres é rejeitada na validação")
    void senhaCurtaERejeitada() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Curta","email":"curta@teste.local","senha":"curta"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tenant existente sem role informada é rejeitado")
    void tenantExistenteExigeRole() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"tenantId":"%s","nome":"Sem Role","email":"semrole@teste.local",
                                 "senha":"SenhaQualquer12"}
                                """.formatted(tenantA.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("e-mail já usado em OUTRO tenant é rejeitado — ele é a credencial de login")
    void emailEUnicoNoSistemaInteiro() throws Exception {
        long tenantsAntes = tenantRepository.count();

        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Clone","email":"%s","senha":"SenhaDoClone12"}
                                """.formatted(adminA.getEmail())))
                .andExpect(status().isConflict());

        // e o tenant não pode ter sido criado antes da recusa
        assertThat(tenantRepository.count()).isEqualTo(tenantsAntes);
    }

    @Test
    @DisplayName("o novo administrador consegue entrar com a senha cadastrada")
    void oClienteNovoConsegueEntrar() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Marina","email":"marina@nova.local","senha":"SenhaMarina12"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"marina@nova.local","senha":"SenhaMarina12"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.tenantNome").value("Marina"));
    }

    @Test
    @DisplayName("tenants homônimos são permitidos — o identificador é o id, não o nome")
    void nomeDeTenantNaoPrecisaSerUnico() throws Exception {
        String token = autenticar(superadmin.getEmail());

        for (String email : new String[]{"silvia1@teste.local", "silvia2@teste.local"}) {
            mockMvc.perform(post("/usuarios")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Silvia Santos","email":"%s","senha":"SenhaSilvia12"}
                                    """.formatted(email)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tenantNome").value("Silvia Santos"));
        }
    }

    private String tokenNoTenantA() throws Exception {
        String corpo = mockMvc.perform(post("/auth/switch-tenant/{id}", tenantA.getId())
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(corpo).get("accessToken").asText();
    }
}
