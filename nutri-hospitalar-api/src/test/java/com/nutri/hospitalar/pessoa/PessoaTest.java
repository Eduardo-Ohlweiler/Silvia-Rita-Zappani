package com.nutri.hospitalar.pessoa;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.catalogo.entity.TipoCadastro;
import com.nutri.hospitalar.catalogo.entity.TipoEmail;
import com.nutri.hospitalar.catalogo.entity.TipoTelefone;
import com.nutri.hospitalar.catalogo.repository.TipoCadastroRepository;
import com.nutri.hospitalar.catalogo.repository.TipoEmailRepository;
import com.nutri.hospitalar.catalogo.repository.TipoTelefoneRepository;
import com.nutri.hospitalar.contato.repository.TelefoneRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro de pessoas.
 *
 * <p>Cobre o que o porte do eroERP trouxe de regra: coerência entre tipo de
 * pessoa e campos, validação e unicidade de documento <b>por tenant</b>, e a
 * sincronização das listas de contato — onde a lista recebida é o estado
 * completo, não um acréscimo.
 */
@DisplayName("Cadastro de pessoas")
class PessoaTest extends AbstractIntegrationTest {

    /** CPFs e CNPJ com dígito verificador válido, de uso corrente em teste. */
    private static final String CPF_VALIDO = "52998224725";
    private static final String CPF_VALIDO_2 = "11144477735";
    private static final String CPF_INVALIDO = "12345678900";
    private static final String CNPJ_VALIDO = "11222333000181";

    @Autowired TipoCadastroRepository tipoCadastroRepository;
    @Autowired TipoTelefoneRepository tipoTelefoneRepository;
    @Autowired TipoEmailRepository tipoEmailRepository;
    @Autowired TelefoneRepository telefoneRepository;

    private UUID tipoPaciente;
    private UUID tipoResponsavel;
    private UUID tipoCelular;
    private UUID tipoParticular;

    @BeforeEach
    void carregarCatalogos() {
        tipoPaciente = idDoTipoCadastro("Paciente");
        tipoResponsavel = idDoTipoCadastro("Responsável");
        tipoCelular = tipoTelefoneRepository.findAllByAtivoTrueOrderByNome().stream()
                .filter(t -> t.getNome().equals("Celular"))
                .map(TipoTelefone::getId).findFirst().orElseThrow();
        tipoParticular = tipoEmailRepository.findAllByAtivoTrueOrderByNome().stream()
                .filter(t -> t.getNome().equals("Particular"))
                .map(TipoEmail::getId).findFirst().orElseThrow();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Cadastro
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cria pessoa física com documento, tipo de cadastro, telefone e e-mail")
    void criaPessoaFisicaCompleta() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"José da Silva","tipoPessoa":"PESSOA_FISICA",
                                 "cpf":"529.982.247-25","dataNascimento":"1970-05-20",
                                 "tiposCadastroIds":["%s"],
                                 "telefones":[{"tipoTelefoneId":"%s","numero":"51999990000"}],
                                 "emails":[{"tipoEmailId":"%s","email":"Jose@Teste.Local"}]}
                                """.formatted(tipoPaciente, tipoCelular, tipoParticular)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("José da Silva"))
                // Documento é gravado só com dígitos, venha como vier da tela
                .andExpect(jsonPath("$.cpf").value(CPF_VALIDO))
                .andExpect(jsonPath("$.tiposCadastro", hasSize(1)))
                .andExpect(jsonPath("$.telefones[0].principal").value(true))
                .andExpect(jsonPath("$.telefones[0].codigoPais").value("55"))
                .andExpect(jsonPath("$.emails[0].email").value("jose@teste.local"));
    }

    @Test
    @DisplayName("aceita pessoa jurídica com CNPJ e razão social")
    void criaPessoaJuridica() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Hospital Central Ltda","tipoPessoa":"PESSOA_JURIDICA",
                                 "cnpj":"11.222.333/0001-81","nomeFantasia":"Hospital Central",
                                 "razaoSocial":"Hospital Central Ltda",
                                 "tiposCadastroIds":["%s"]}
                                """.formatted(tipoPaciente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value(CNPJ_VALIDO))
                .andExpect(jsonPath("$.nomeFantasia").value("Hospital Central"));
    }

    @Test
    @DisplayName("sem tipo de cadastro é recusado")
    void semTipoCadastroFalha() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Sem tipo","tipoPessoa":"PESSOA_FISICA","tiposCadastroIds":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Documento
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CPF com dígito verificador errado é recusado")
    void cpfInvalidoFalha() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(pessoaFisicaJson("Maria", CPF_INVALIDO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CPF repetido no mesmo tenant é conflito")
    void cpfRepetidoNoTenantFalha() throws Exception {
        String token = autenticar(adminA.getEmail());

        mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content(pessoaFisicaJson("José", CPF_VALIDO)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content(pessoaFisicaJson("Outro José", CPF_VALIDO)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o mesmo CPF em outro tenant é aceito — a unicidade é por cliente")
    void mesmoCpfEmOutroTenantPassa() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(pessoaFisicaJson("José no A", CPF_VALIDO)))
                .andExpect(status().isCreated());

        // Dois clientes podem atender a mesma pessoa. Diferente do e-mail de
        // usuário, que é credencial de login e por isso é único no sistema.
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                        .contentType("application/json")
                        .content(pessoaFisicaJson("José no B", CPF_VALIDO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("pessoa física com CNPJ e pessoa jurídica com CPF são recusadas")
    void camposDoTipoErradoFalham() throws Exception {
        String token = autenticar(adminA.getEmail());

        mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Confuso","tipoPessoa":"PESSOA_FISICA",
                                 "cnpj":"11222333000181","tiposCadastroIds":["%s"]}
                                """.formatted(tipoPaciente)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Confusa Ltda","tipoPessoa":"PESSOA_JURIDICA",
                                 "cpf":"52998224725","tiposCadastroIds":["%s"]}
                                """.formatted(tipoPaciente)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("trocar de física para jurídica limpa CPF, RG e nascimento")
    void trocaDeTipoLimpaOsCamposAnteriores() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID id = criarPessoaFisica(token, "José", CPF_VALIDO);

        mockMvc.perform(put("/pessoas/{id}", id).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José Transportes Ltda","tipoPessoa":"PESSOA_JURIDICA",
                                 "cnpj":"11222333000181","tiposCadastroIds":["%s"]}
                                """.formatted(tipoPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value(CNPJ_VALIDO));

        Pessoa recarregada = pessoaRepository.findById(id).orElseThrow();
        assertThat(recarregada.getCpf()).isNull();
        assertThat(recarregada.getRg()).isNull();
        assertThat(recarregada.getDataNascimento()).isNull();
        assertThat(recarregada.getTipoPessoa()).isEqualTo(TipoPessoa.PESSOA_JURIDICA);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Contatos
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dois telefones marcados como principal são recusados")
    void doisPrincipaisFalha() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","tiposCadastroIds":["%s"],
                                 "telefones":[
                                   {"tipoTelefoneId":"%s","numero":"51999990000","principal":true},
                                   {"tipoTelefoneId":"%s","numero":"51988880000","principal":true}]}
                                """.formatted(tipoPaciente, tipoCelular, tipoCelular)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("nenhum marcado: o primeiro da lista vira o principal")
    void semPrincipalOPrimeiroAssume() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","tiposCadastroIds":["%s"],
                                 "telefones":[
                                   {"tipoTelefoneId":"%s","numero":"51999990000"},
                                   {"tipoTelefoneId":"%s","numero":"51988880000"}]}
                                """.formatted(tipoPaciente, tipoCelular, tipoCelular)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefones", hasSize(2)))
                .andExpect(jsonPath("$.telefones[0].principal").value(true))
                .andExpect(jsonPath("$.telefones[1].principal").value(false));
    }

    @Test
    @DisplayName("a lista enviada é o estado completo: o que não vem é removido, o que tem id é alterado")
    void listaDeContatosSubstituiOEstado() throws Exception {
        String token = autenticar(adminA.getEmail());

        String corpo = mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","tiposCadastroIds":["%s"],
                                 "telefones":[
                                   {"tipoTelefoneId":"%s","numero":"51999990000"},
                                   {"tipoTelefoneId":"%s","numero":"51988880000"}]}
                                """.formatted(tipoPaciente, tipoCelular, tipoCelular)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID pessoaId = UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
        String telefoneId = objectMapper.readTree(corpo).get("telefones").get(0).get("id").asText();

        // Só o primeiro volta, com número novo — o segundo tem de sumir
        mockMvc.perform(put("/pessoas/{id}", pessoaId).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","tiposCadastroIds":["%s"],
                                 "telefones":[
                                   {"id":"%s","tipoTelefoneId":"%s","numero":"51977770000","principal":true}]}
                                """.formatted(tipoPaciente, telefoneId, tipoCelular)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefones", hasSize(1)))
                .andExpect(jsonPath("$.telefones[0].numero").value("51977770000"));

        assertThat(telefoneRepository.findAllByPessoaIdAndTenantId(pessoaId, tenantA.getId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("lista de contatos ausente apaga os que existiam")
    void listaAusenteApagaOsContatos() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID id = criarPessoaFisica(token, "José", CPF_VALIDO);

        mockMvc.perform(put("/pessoas/{id}", id).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"]}
                                """.formatted(CPF_VALIDO, tipoPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefones", hasSize(0)));

        assertThat(telefoneRepository.findAllByPessoaIdAndTenantId(id, tenantA.getId())).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Isolamento e acesso
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("pessoa de outro tenant devolve 404, não 403")
    void pessoaDeOutroTenantNaoAparece() throws Exception {
        UUID idNoA = criarPessoaFisica(autenticar(adminA.getEmail()), "José", CPF_VALIDO);

        mockMvc.perform(get("/pessoas/{id}", idNoA)
                        .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/pessoas").header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("USER opera o cadastro — é módulo de negócio, não área administrativa")
    void usuarioComumOperaOCadastro() throws Exception {
        String token = autenticar(userA.getEmail());

        mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content(pessoaFisicaJson("José", CPF_VALIDO)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/pessoas").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ─────────────────────────────────────────────────────────────────
    //  Consulta
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("busca por nome ignora acento e por documento aceita parte")
    void filtrosDeNomeEDocumento() throws Exception {
        String token = autenticar(adminA.getEmail());
        criarPessoaFisica(token, "José da Silva", CPF_VALIDO);

        mockMvc.perform(get("/pessoas").header(AUTHORIZATION, token).param("nome", "jose"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/pessoas").header(AUTHORIZATION, token).param("documento", "982247"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Com máscara também acha: o filtro é reduzido a dígitos antes da busca
        mockMvc.perform(get("/pessoas").header(AUTHORIZATION, token)
                        .param("documento", "529.982"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("filtro por tipo de cadastro restringe a quem tem aquele tipo")
    void filtroPorTipoDeCadastro() throws Exception {
        String token = autenticar(adminA.getEmail());
        criarPessoaFisica(token, "Paciente José", CPF_VALIDO);

        mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Responsável Maria","tipoPessoa":"PESSOA_FISICA",
                                 "cpf":"%s","tiposCadastroIds":["%s"]}
                                """.formatted(CPF_VALIDO_2, tipoResponsavel)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/pessoas").header(AUTHORIZATION, token)
                        .param("tipoCadastroId", tipoResponsavel.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Responsável Maria"));
    }

    @Test
    @DisplayName("inativar não apaga: a pessoa some do filtro de ativas e continua no cadastro")
    void inativarMantemOCadastro() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID id = criarPessoaFisica(token, "José", CPF_VALIDO);

        mockMvc.perform(patch("/pessoas/{id}/ativo", id).header(AUTHORIZATION, token)
                        .param("ativo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(get("/pessoas").header(AUTHORIZATION, token).param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        assertThat(pessoaRepository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("o select traz só as ativas do tenant, com documento para desempatar homônimo")
    void selectTrazAtivasComDocumento() throws Exception {
        String token = autenticar(adminA.getEmail());
        criarPessoaFisica(token, "José da Silva", CPF_VALIDO);

        mockMvc.perform(get("/pessoas/select").header(AUTHORIZATION, token).param("termo", "silva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documento").value(CPF_VALIDO));
    }

    @Test
    @DisplayName("os catálogos vêm semeados e são iguais para todo cliente")
    void catalogosSemeados() throws Exception {
        mockMvc.perform(get("/tipos-cadastro/select")
                        .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        mockMvc.perform(get("/tipos-telefone/select")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    // ─────────────────────────────────────────────────────────────────

    private UUID idDoTipoCadastro(String nome) {
        return tipoCadastroRepository.findAllByAtivoTrueOrderByNome().stream()
                .filter(t -> t.getNome().equals(nome))
                .map(TipoCadastro::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed sem o tipo " + nome));
    }

    private String pessoaFisicaJson(String nome, String cpf) {
        return """
                {"nome":"%s","tipoPessoa":"PESSOA_FISICA","cpf":"%s","tiposCadastroIds":["%s"],
                 "telefones":[{"tipoTelefoneId":"%s","numero":"51999990000"}]}
                """.formatted(nome, cpf, tipoPaciente, tipoCelular);
    }

    private UUID criarPessoaFisica(String token, String nome, String cpf) throws Exception {
        String corpo = mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content(pessoaFisicaJson(nome, cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }
}
