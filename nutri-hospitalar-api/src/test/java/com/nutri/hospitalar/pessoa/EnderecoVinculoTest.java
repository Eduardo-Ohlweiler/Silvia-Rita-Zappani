package com.nutri.hospitalar.pessoa;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.catalogo.entity.TipoCadastro;
import com.nutri.hospitalar.catalogo.entity.TipoEndereco;
import com.nutri.hospitalar.catalogo.repository.TipoCadastroRepository;
import com.nutri.hospitalar.catalogo.repository.TipoEnderecoRepository;
import com.nutri.hospitalar.contato.repository.EnderecoRepository;
import com.nutri.hospitalar.localidade.dtos.CidadeSelectDto;
import com.nutri.hospitalar.localidade.repository.CidadeRepository;
import com.nutri.hospitalar.localidade.repository.EstadoRepository;
import com.nutri.hospitalar.localidade.service.LocalidadeService;
import com.nutri.hospitalar.vinculo.repository.PessoaVinculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endereços e vínculos — a etapa B do cadastro de pessoas.
 *
 * <p>O que precisa ficar provado: a cidade vem da tabela do IBGE e não de texto
 * livre, e o vínculo é <b>uma linha só servindo os dois cadastros</b> — cadastrar
 * "Maria é responsável de José" faz José aparecer como dependente no cadastro
 * de Maria, sem segunda linha e sem digitar de novo.
 */
@DisplayName("Endereços e vínculos")
class EnderecoVinculoTest extends AbstractIntegrationTest {

    private static final String CPF_JOSE = "52998224725";
    private static final String CPF_MARIA = "11144477735";
    private static final String CPF_ANA = "12345678909";

    @Autowired TipoCadastroRepository tipoCadastroRepository;
    @Autowired TipoEnderecoRepository tipoEnderecoRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired CidadeRepository cidadeRepository;
    @Autowired EnderecoRepository enderecoRepository;
    @Autowired PessoaVinculoRepository vinculoRepository;
    @Autowired LocalidadeService localidadeService;

    private UUID tipoPaciente;
    private UUID tipoResidencial;
    private UUID portoAlegre;

    @BeforeEach
    void carregarReferencias() {
        tipoPaciente = tipoCadastroRepository.findAllByAtivoTrueOrderByNome().stream()
                .filter(t -> t.getNome().equals("Paciente"))
                .map(TipoCadastro::getId).findFirst().orElseThrow();

        tipoResidencial = tipoEnderecoRepository.findAllByAtivoTrueOrderByNome().stream()
                .filter(t -> t.getNome().equals("Residencial"))
                .map(TipoEndereco::getId).findFirst().orElseThrow();

        portoAlegre = localidadeService.cidades("Porto Alegre", null).stream()
                .filter(c -> c.estadoSigla().equals("RS"))
                .map(CidadeSelectDto::id).findFirst().orElseThrow();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Seed do IBGE
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("o seed traz as 27 UFs e os municípios ligados ao estado certo")
    void seedDoIbgeEstaCompleto() {
        assertThat(estadoRepository.count()).isEqualTo(27);
        assertThat(cidadeRepository.count()).isGreaterThan(5500);

        assertThat(cidadeRepository.findById(portoAlegre).orElseThrow().getCodigoIbge())
                .isEqualTo(4314902);
    }

    @Test
    @DisplayName("a busca de cidade ignora acento e a UF desempata nome repetido")
    void buscaDeCidadeIgnoraAcentoEFiltraPorUf() throws Exception {
        String token = autenticar(adminA.getEmail());

        mockMvc.perform(get("/cidades/select").header(AUTHORIZATION, token)
                        .param("nome", "brasilia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Brasília"))
                .andExpect(jsonPath("$[0].estadoSigla").value("DF"));

        // "Bom Jesus" existe em vários estados — sem a UF, vem mais de um
        String semUf = mockMvc.perform(get("/cidades/select").header(AUTHORIZATION, token)
                        .param("nome", "Bom Jesus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(semUf).size()).isGreaterThan(1);

        UUID rs = idDoEstado("RS");
        mockMvc.perform(get("/cidades/select").header(AUTHORIZATION, token)
                        .param("nome", "Bom Jesus").param("estadoId", rs.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estadoSigla").value("RS"));
    }

    // ─────────────────────────────────────────────────────────────────
    //  Endereço
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("endereço é gravado com a cidade do IBGE e volta com a UF")
    void enderecoGuardaCidadeEVoltaComUf() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "enderecos":[{"tipoEnderecoId":"%s","cidadeId":"%s",
                                   "cep":"90010-000","rua":"Rua dos Andradas","numero":"100",
                                   "bairro":"Centro"}]}
                                """.formatted(CPF_JOSE, tipoPaciente, tipoResidencial, portoAlegre)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enderecos", hasSize(1)))
                .andExpect(jsonPath("$.enderecos[0].cidadeNome").value("Porto Alegre"))
                .andExpect(jsonPath("$.enderecos[0].estadoSigla").value("RS"))
                // CEP guardado só com dígitos, como os demais documentos
                .andExpect(jsonPath("$.enderecos[0].cep").value("90010000"))
                .andExpect(jsonPath("$.enderecos[0].principal").value(true));
    }

    @Test
    @DisplayName("dois endereços principais são recusados")
    void doisEnderecosPrincipaisFalha() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","tiposCadastroIds":["%s"],
                                 "enderecos":[
                                   {"tipoEnderecoId":"%s","cidadeId":"%s","principal":true},
                                   {"tipoEnderecoId":"%s","cidadeId":"%s","principal":true}]}
                                """.formatted(tipoPaciente, tipoResidencial, portoAlegre,
                                tipoResidencial, portoAlegre)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cidade inexistente devolve 404")
    void cidadeInexistenteFalha() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","tiposCadastroIds":["%s"],
                                 "enderecos":[{"tipoEnderecoId":"%s","cidadeId":"%s"}]}
                                """.formatted(tipoPaciente, tipoResidencial, UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update sem o endereço que existia o remove")
    void enderecoAusenteNoUpdateSomeDoBanco() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID id = criarComEndereco(token);

        mockMvc.perform(put("/pessoas/{id}", id).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"]}
                                """.formatted(CPF_JOSE, tipoPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enderecos", hasSize(0)));

        assertThat(enderecoRepository.findAllByPessoaIdAndTenantId(id, tenantA.getId())).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Vínculo — a reciprocidade
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("vincular Maria como responsável de José faz José aparecer como dependente dela")
    void vinculoApareceNosDoisCadastrosComORotuloInvertido() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID maria = criarPessoa(token, "Maria", CPF_MARIA);
        UUID jose = criarPessoaComVinculo(token, "José", CPF_JOSE, maria, "RESPONSAVEL");

        // No cadastro do José, Maria é responsável
        mockMvc.perform(get("/pessoas/{id}", jose).header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos", hasSize(1)))
                .andExpect(jsonPath("$.vinculos[0].pessoaNome").value("Maria"))
                .andExpect(jsonPath("$.vinculos[0].tipo").value("RESPONSAVEL"));

        // No cadastro da Maria, José é dependente — sem ter sido cadastrado lá
        mockMvc.perform(get("/pessoas/{id}", maria).header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos", hasSize(1)))
                .andExpect(jsonPath("$.vinculos[0].pessoaNome").value("José"))
                .andExpect(jsonPath("$.vinculos[0].tipo").value("DEPENDENTE"))
                .andExpect(jsonPath("$.vinculos[0].tipoDescricao").value("Dependente"));

        // Uma aresta só no banco
        assertThat(vinculoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("cônjuge é simétrico: mesmo rótulo dos dois lados")
    void conjugeEhSimetrico() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID maria = criarPessoa(token, "Maria", CPF_MARIA);
        UUID jose = criarPessoaComVinculo(token, "José", CPF_JOSE, maria, "CONJUGE");

        mockMvc.perform(get("/pessoas/{id}", jose).header(AUTHORIZATION, token))
                .andExpect(jsonPath("$.vinculos[0].tipo").value("CONJUGE"));
        mockMvc.perform(get("/pessoas/{id}", maria).header(AUTHORIZATION, token))
                .andExpect(jsonPath("$.vinculos[0].tipo").value("CONJUGE"));
    }

    @Test
    @DisplayName("remover o vínculo por um cadastro remove do outro — é uma relação, não uma anotação")
    void removerDeUmLadoRemoveDoOutro() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID maria = criarPessoa(token, "Maria", CPF_MARIA);
        UUID jose = criarPessoaComVinculo(token, "José", CPF_JOSE, maria, "RESPONSAVEL");

        // Salva a Maria sem vínculo nenhum
        mockMvc.perform(put("/pessoas/{id}", maria).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Maria","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"]}
                                """.formatted(CPF_MARIA, tipoPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos", hasSize(0)));

        mockMvc.perform(get("/pessoas/{id}", jose).header(AUTHORIZATION, token))
                .andExpect(jsonPath("$.vinculos", hasSize(0)));
        assertThat(vinculoRepository.count()).isZero();
    }

    @Test
    @DisplayName("editar o tipo pelo outro lado mantém os dois cadastros coerentes")
    void editarPeloOutroLadoGravaOInverso() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID maria = criarPessoa(token, "Maria", CPF_MARIA);
        UUID jose = criarPessoaComVinculo(token, "José", CPF_JOSE, maria, "RESPONSAVEL");

        String corpo = mockMvc.perform(get("/pessoas/{id}", maria).header(AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        String vinculoId = objectMapper.readTree(corpo).get("vinculos").get(0).get("id").asText();

        // Pelo cadastro da Maria, José passa a ser FAMILIAR
        mockMvc.perform(put("/pessoas/{id}", maria).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Maria","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "vinculos":[{"id":"%s","pessoaId":"%s","tipo":"FAMILIAR"}]}
                                """.formatted(CPF_MARIA, tipoPaciente, vinculoId, jose)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos[0].tipo").value("FAMILIAR"));

        mockMvc.perform(get("/pessoas/{id}", jose).header(AUTHORIZATION, token))
                .andExpect(jsonPath("$.vinculos[0].tipo").value("FAMILIAR"));
    }

    @Test
    @DisplayName("trocar um vínculo por outro no mesmo PUT não esbarra na unique")
    void trocarVinculoNoMesmoPut() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID maria = criarPessoa(token, "Maria", CPF_MARIA);
        UUID ana = criarPessoa(token, "Ana", CPF_ANA);
        UUID jose = criarPessoaComVinculo(token, "José", CPF_JOSE, maria, "RESPONSAVEL");

        mockMvc.perform(put("/pessoas/{id}", jose).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "vinculos":[{"pessoaId":"%s","tipo":"RESPONSAVEL"}]}
                                """.formatted(CPF_JOSE, tipoPaciente, ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos", hasSize(1)))
                .andExpect(jsonPath("$.vinculos[0].pessoaNome").value("Ana"));

        assertThat(vinculoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("vincular a pessoa a ela mesma é recusado")
    void vinculoConsigoMesmaFalha() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID jose = criarPessoa(token, "José", CPF_JOSE);

        mockMvc.perform(put("/pessoas/{id}", jose).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "vinculos":[{"pessoaId":"%s","tipo":"RESPONSAVEL"}]}
                                """.formatted(CPF_JOSE, tipoPaciente, jose)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a mesma pessoa duas vezes na lista é recusada")
    void parRepetidoNaMesmaListaFalha() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID maria = criarPessoa(token, "Maria", CPF_MARIA);
        UUID jose = criarPessoa(token, "José", CPF_JOSE);

        mockMvc.perform(put("/pessoas/{id}", jose).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "vinculos":[
                                   {"pessoaId":"%s","tipo":"RESPONSAVEL"},
                                   {"pessoaId":"%s","tipo":"CONJUGE"}]}
                                """.formatted(CPF_JOSE, tipoPaciente, maria, maria)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reenviar o mesmo par sem id substitui o vínculo — a lista é o estado completo")
    void reenviarOParSemIdSubstitui() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID maria = criarPessoa(token, "Maria", CPF_MARIA);
        UUID jose = criarPessoaComVinculo(token, "José", CPF_JOSE, maria, "RESPONSAVEL");

        // Sem o id, o vínculo antigo sai e outro entra no lugar: não é duplicata
        mockMvc.perform(put("/pessoas/{id}", jose).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "vinculos":[{"pessoaId":"%s","tipo":"CONJUGE"}]}
                                """.formatted(CPF_JOSE, tipoPaciente, maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos", hasSize(1)))
                .andExpect(jsonPath("$.vinculos[0].tipo").value("CONJUGE"));

        assertThat(vinculoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("vínculo com pessoa de outro tenant devolve 404")
    void vinculoCrossTenantFalha() throws Exception {
        UUID noTenantB = criarPessoa(autenticar(adminB.getEmail()), "Maria do B", CPF_MARIA);

        String token = autenticar(adminA.getEmail());
        UUID jose = criarPessoa(token, "José", CPF_JOSE);

        mockMvc.perform(put("/pessoas/{id}", jose).header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "vinculos":[{"pessoaId":"%s","tipo":"RESPONSAVEL"}]}
                                """.formatted(CPF_JOSE, tipoPaciente, noTenantB)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("o combo de pessoas não oferece a própria pessoa quando se pede para ignorá-la")
    void selectIgnoraAPropriaPessoa() throws Exception {
        String token = autenticar(adminA.getEmail());
        UUID jose = criarPessoa(token, "José", CPF_JOSE);
        criarPessoa(token, "Maria", CPF_MARIA);

        mockMvc.perform(get("/pessoas/select").header(AUTHORIZATION, token)
                        .param("ignorarId", jose.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Maria"));
    }

    // ─────────────────────────────────────────────────────────────────

    private UUID idDoEstado(String sigla) {
        return estadoRepository.findAllByAtivoTrueOrderByNome().stream()
                .filter(e -> e.getSigla().equals(sigla))
                .findFirst().orElseThrow().getId();
    }

    private UUID criarPessoa(String token, String nome, String cpf) throws Exception {
        String corpo = mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"%s","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"]}
                                """.formatted(nome, cpf, tipoPaciente)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }

    private UUID criarPessoaComVinculo(String token, String nome, String cpf,
                                       UUID outra, String tipo) throws Exception {
        String corpo = mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"%s","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "vinculos":[{"pessoaId":"%s","tipo":"%s"}]}
                                """.formatted(nome, cpf, tipoPaciente, outra, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }

    private UUID criarComEndereco(String token) throws Exception {
        String corpo = mockMvc.perform(post("/pessoas").header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"José","tipoPessoa":"PESSOA_FISICA","cpf":"%s",
                                 "tiposCadastroIds":["%s"],
                                 "enderecos":[{"tipoEnderecoId":"%s","cidadeId":"%s",
                                   "rua":"Rua dos Andradas"}]}
                                """.formatted(CPF_JOSE, tipoPaciente, tipoResidencial, portoAlegre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }
}
