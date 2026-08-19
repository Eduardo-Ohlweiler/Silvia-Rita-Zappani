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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Avaliação pediátrica")
class AvaliacaoPediatricaTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    private Pessoa pacienteA;
    private UUID nan2;

    @BeforeEach
    void cenario() throws Exception {
        pacienteA = criarPaciente(tenantA, "Maria Pediatria");
        nan2 = idDaFormulaGlobal("NAN 2");
    }

    // ─── O caso da planilha, ponta a ponta ───────────────────────────────

    @Test
    @DisplayName("calcula sem gravar: o caso da planilha chega inteiro pelo HTTP")
    void calcularSemGravar() throws Exception {
        mockMvc.perform(post("/pediatria/avaliacoes/calcular")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"FEMININO","idadeMeses":8,"peso":9,
                                 "formulaLacteaId":"%s","volumeMl":110,"frequenciaHoras":3}
                                """.formatted(nan2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNutricional.pesoIdade.faixa").value("ADEQUADA"))
                .andExpect(jsonPath("$.estadoNutricional.pesoIdade.rotulo").value("Peso adequado"))
                .andExpect(jsonPath("$.necessidades.vet").value(723.0000))
                .andExpect(jsonPath("$.necessidades.proteina").value(11.0000))
                .andExpect(jsonPath("$.dieta.vezesDia").value(8.0000))
                .andExpect(jsonPath("$.dieta.volumeTotal").value(880.0000))
                .andExpect(jsonPath("$.dieta.caloriasTotais").value(649.4400))
                .andExpect(jsonPath("$.dieta.proteinaTotal").value(14.5200))
                .andExpect(jsonPath("$.dieta.percCalorico").value(89.83))
                .andExpect(jsonPath("$.dieta.percProteico").value(132.00));

        // Calcular não grava nada
        assertThat(avaliacaoPediatricaRepository.count()).isZero();
    }

    @Test
    @DisplayName("sem estatura, o IMC vem ausente com o motivo escrito")
    void motivoDeAusencia() throws Exception {
        mockMvc.perform(post("/pediatria/avaliacoes/calcular")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"FEMININO","idadeMeses":8,"peso":9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNutricional.imc").doesNotExist())
                .andExpect(jsonPath("$.estadoNutricional.motivoImc").value(
                        "Informe a estatura para calcular o IMC"));
    }

    @Test
    @DisplayName("aos 40 meses não há VET, e a resposta diz por quê")
    void foraDaFaixaDoVet() throws Exception {
        mockMvc.perform(post("/pediatria/avaliacoes/calcular")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"FEMININO","idadeMeses":40,"peso":15,"estatura":98}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.necessidades.vet").doesNotExist())
                .andExpect(jsonPath("$.necessidades.motivoVet").value(
                        "O VET das DRIs 2002 é definido até 35 meses"))
                // O estado nutricional vai até 60 meses: continua saindo
                .andExpect(jsonPath("$.estadoNutricional.pesoIdade.faixa").exists());
    }

    // ─── Gravação ────────────────────────────────────────────────────────

    @Test
    @DisplayName("grava a avaliação com o resultado calculado no servidor")
    void grava() throws Exception {
        mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpoCanonico(pacienteA.getId(), nan2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("Maria Pediatria"))
                .andExpect(jsonPath("$.resultado.necessidades.vet").value(723.0000))
                .andExpect(jsonPath("$.resultado.dieta.caloriasTotais").value(649.4400))
                // Retrato da fórmula, tirado na hora do cálculo
                .andExpect(jsonPath("$.formulaNome").value("NAN 2"))
                .andExpect(jsonPath("$.formulaKcalPor100ml").value(73.800));
    }

    @Test
    @DisplayName("resultado enviado pelo cliente é ignorado — o servidor grava o seu")
    void naoAceitaResultadoDoCliente() throws Exception {
        // O DTO de escrita nem declara esses campos. O teste existe para que,
        // se alguém os acrescentar um dia, a suíte reclame: no eroERP o backend
        // gravava o que o front mandasse, e um cliente adulterado escrevia
        // imc: 999 no prontuário.
        mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"2026-08-18",
                                 "sexo":"FEMININO","idadeMeses":8,"peso":9,
                                 "formulaLacteaId":"%s","volumeMl":110,"frequenciaHoras":3,
                                 "imc":999,"vet":99999,"percCalorico":1,
                                 "classifPesoIdade":"ALTA"}
                                """.formatted(pacienteA.getId(), nan2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado.necessidades.vet").value(723.0000))
                .andExpect(jsonPath("$.resultado.estadoNutricional.imc").doesNotExist())
                .andExpect(jsonPath("$.resultado.estadoNutricional.pesoIdade.faixa").value("ADEQUADA"));
    }

    @Test
    @DisplayName("abrir uma avaliação salva devolve o que foi gravado, sem motivos")
    void abreSemRecalcular() throws Exception {
        String id = criarAvaliacao();

        mockMvc.perform(get("/pediatria/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado.necessidades.vet").value(723.0000))
                // Motivo explica o instante da digitação; o registro só mostra
                // o que foi calculado. Ver ResultadoPediatricoDto.
                .andExpect(jsonPath("$.resultado.necessidades.motivoVet").doesNotExist());
    }

    @Test
    @DisplayName("alterar o peso refaz o cálculo inteiro")
    void alterarRecalcula() throws Exception {
        String id = criarAvaliacao();

        mockMvc.perform(put("/pediatria/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"2026-08-18",
                                 "sexo":"FEMININO","idadeMeses":8,"peso":6,
                                 "formulaLacteaId":"%s","volumeMl":110,"frequenciaHoras":3}
                                """.formatted(pacienteA.getId(), nan2)))
                .andExpect(status().isOk())
                // 6 kg está abaixo do P15 de 7,0 aos 8 meses
                .andExpect(jsonPath("$.resultado.estadoNutricional.pesoIdade.faixa").value("BAIXA"))
                .andExpect(jsonPath("$.resultado.estadoNutricional.pesoIdade.rotulo").value("Baixo peso"))
                // VET acompanha: (89×6−100)+22 = 456
                .andExpect(jsonPath("$.resultado.necessidades.vet").value(456.0000));
    }

    @Test
    @DisplayName("desativar a fórmula não altera avaliação já gravada")
    void retratoSobreviveAoCatalogo() throws Exception {
        String id = criarAvaliacao();
        UUID propria = criarFormulaPropria();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/formulas-lacteas/" + propria + "/ativo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .param("ativo", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/pediatria/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaNome").value("NAN 2"))
                .andExpect(jsonPath("$.resultado.dieta.caloriasTotais").value(649.4400));
    }

    @Test
    @DisplayName("paciente é obrigatório")
    void pacienteObrigatorio() throws Exception {
        mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"dataAvaliacao":"2026-08-18","sexo":"FEMININO",
                                 "idadeMeses":8,"peso":9}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────────

    private String criarAvaliacao() throws Exception {
        String corpo = mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpoCanonico(pacienteA.getId(), nan2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    private UUID criarFormulaPropria() throws Exception {
        String corpo = mockMvc.perform(post("/formulas-lacteas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fórmula da casa","kcalPor100ml":70,"proteinaPor100ml":1.5}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }

    static String corpoCanonico(UUID pacienteId, UUID formulaId) {
        return """
                {"pacienteId":"%s","dataAvaliacao":"2026-08-18",
                 "sexo":"FEMININO","idadeMeses":8,"peso":9,
                 "formulaLacteaId":"%s","volumeMl":110,"frequenciaHoras":3}
                """.formatted(pacienteId, formulaId);
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

    private UUID idDaFormulaGlobal(String nome) {
        return formulaLacteaRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && nome.equals(f.getNome()))
                .findFirst().orElseThrow().getId();
    }
}
