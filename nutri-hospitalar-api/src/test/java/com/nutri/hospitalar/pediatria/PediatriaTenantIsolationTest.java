package com.nutri.hospitalar.pediatria;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.catalogo.repository.TipoCadastroRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.tenant.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolamento por tenant na pediatria — a regra máxima do sistema.
 *
 * <p>Cobre também a exceção deliberada: a fórmula láctea global, que atravessa
 * tenants <b>na leitura</b> e por nenhum é editável.
 */
@DisplayName("Isolamento por tenant na pediatria")
class PediatriaTenantIsolationTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    private Pessoa pacienteA;
    private Pessoa pacienteB;
    private UUID nan2;

    @BeforeEach
    void cenario() {
        pacienteA = criarPaciente(tenantA, "Paciente do A");
        pacienteB = criarPaciente(tenantB, "Paciente do B");
        nan2 = formulaGlobal("NAN 2");
    }

    // ─── Avaliação ───────────────────────────────────────────────────────

    @Test
    @DisplayName("avaliação de outro cliente devolve 404 na leitura")
    void naoLeDeOutroTenant() throws Exception {
        String id = criarAvaliacaoNoTenantA();

        mockMvc.perform(get("/pediatria/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("avaliação de outro cliente devolve 404 na alteração")
    void naoAlteraDeOutroTenant() throws Exception {
        String id = criarAvaliacaoNoTenantA();

        mockMvc.perform(put("/pediatria/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                        .contentType("application/json")
                        .content(AvaliacaoPediatricaTest.corpoCanonico(pacienteB.getId(), nan2)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a listagem só enxerga o próprio cliente")
    void listaSoDoProprioTenant() throws Exception {
        criarAvaliacaoNoTenantA();

        mockMvc.perform(get("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("não dá para gravar avaliação com paciente de outro cliente")
    void naoUsaPacienteDeOutroTenant() throws Exception {
        // O tenantId nunca vem do frontend; o pacienteId vem, e por isso é
        // resolvido SEMPRE dentro do tenant do token.
        mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(AvaliacaoPediatricaTest.corpoCanonico(pacienteB.getId(), nan2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Paciente não encontrado"));
    }

    // ─── Fórmula láctea: a exceção deliberada ────────────────────────────

    @Test
    @DisplayName("a fórmula global é visível para os dois clientes")
    void globalEhVisivelParaTodos() throws Exception {
        for (String email : new String[]{adminA.getEmail(), adminB.getEmail()}) {
            mockMvc.perform(get("/formulas-lacteas/" + nan2)
                            .header(AUTHORIZATION, autenticar(email)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("NAN 2"))
                    .andExpect(jsonPath("$.global").value(true));
        }
    }

    @Test
    @DisplayName("a fórmula global não é editável por nenhum cliente, com 400 explicado")
    void globalNaoEhEditavel() throws Exception {
        mockMvc.perform(put("/formulas-lacteas/" + nan2)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"NAN 2 adulterado","kcalPor100ml":1,"proteinaPor100ml":1}
                                """))
                .andExpect(status().isBadRequest())
                // 400 e não 404: o registro está bem à frente do usuário na
                // lista, e mandá-lo procurar seria mentira.
                .andExpect(jsonPath("$.erro").value(
                        org.hamcrest.Matchers.containsString("fórmula do sistema")));

        mockMvc.perform(patch("/formulas-lacteas/" + nan2 + "/ativo")
                        .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                        .param("ativo", "false"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fórmula própria de um cliente não aparece para o outro")
    void formulaPropriaNaoVazaEntreTenants() throws Exception {
        String corpo = mockMvc.perform(post("/formulas-lacteas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fórmula só do A","kcalPor100ml":70,"proteinaPor100ml":1.5}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asText();

        mockMvc.perform(get("/formulas-lacteas/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.global").value(false));

        mockMvc.perform(get("/formulas-lacteas/" + id)
                        .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("o combo do B traz as globais mas nenhuma fórmula do A")
    void comboNaoVazaEntreTenants() throws Exception {
        mockMvc.perform(post("/formulas-lacteas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Exclusiva do A","kcalPor100ml":70,"proteinaPor100ml":1.5}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/formulas-lacteas/select")
                        .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(status().isOk())
                // As 10 globais da migration 016, e só elas
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[?(@.nome == 'Exclusiva do A')]").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────────

    private String criarAvaliacaoNoTenantA() throws Exception {
        String corpo = mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(AvaliacaoPediatricaTest.corpoCanonico(pacienteA.getId(), nan2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    private Pessoa criarPaciente(Tenant tenant, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setTenant(tenant);
        pessoa.setNome(nome);
        pessoa.setTipoPessoa(TipoPessoa.PESSOA_FISICA);
        pessoa.setSexo(Sexo.FEMININO);
        pessoa.getTiposCadastro().add(tipoCadastroRepository.findAll().stream()
                .filter(t -> "Paciente".equals(t.getNome()))
                .findFirst().orElseThrow());
        return pessoaRepository.save(pessoa);
    }

    private UUID formulaGlobal(String nome) {
        return formulaLacteaRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && nome.equals(f.getNome()))
                .findFirst().orElseThrow().getId();
    }
}
