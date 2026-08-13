package com.nutri.hospitalar.tenant;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.jobs.ManutencaoJob;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.enums.PeriodoAcesso;
import com.nutri.hospitalar.usuario.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Período de acesso — a licença do cliente.
 *
 * <p>O sistema é vendido como produto: quem paga é o cliente, e o prazo mora
 * no tenant. Este teste cobre as três coisas que não podem falhar — o prazo
 * nascer junto do cliente, a rotina desligar quem venceu sem nunca desligar o
 * tenant raiz, e a renovação ser a única porta de volta.
 */
@DisplayName("Período de acesso do cliente")
class PeriodoAcessoTest extends AbstractIntegrationTest {

    @Autowired ManutencaoJob manutencaoJob;

    // ─────────────────────────────────────────────────────────────────
    //  O prazo nasce com o cliente
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cliente novo com período de 1 mês nasce com a data de expiração calculada")
    void clienteNovoNasceComPrazo() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Carla Nutricionista","email":"carla@nova.local",
                                 "senha":"SenhaDaCarla12","periodoAcesso":"UM_MES"}
                                """))
                .andExpect(status().isCreated());

        Tenant novo = tenantDe("carla@nova.local");

        assertThat(novo.getPeriodoAcesso()).isEqualTo(PeriodoAcesso.UM_MES);
        assertThat(novo.getAcessoExpiraEm()).isNotNull();
        assertThat(novo.acessoExpirado()).isFalse();

        // Fim do dia, um mês à frente — a tolerância cobre o fuso da máquina.
        long diasAteExpirar = ChronoUnit.DAYS.between(
                LocalDate.now(), novo.getAcessoExpiraEm().atZone(java.time.ZoneOffset.UTC).toLocalDate());
        assertThat(diasAteExpirar).isBetween(27L, 32L);
    }

    @Test
    @DisplayName("sem período informado o cliente nasce indeterminado, sem data")
    void semPeriodoNasceIndeterminado() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Bruno","email":"bruno@novo.local","senha":"SenhaDoBruno12"}
                                """))
                .andExpect(status().isCreated());

        Tenant novo = tenantDe("bruno@novo.local");

        assertThat(novo.getPeriodoAcesso()).isEqualTo(PeriodoAcesso.INDETERMINADO);
        assertThat(novo.getAcessoExpiraEm()).isNull();
    }

    @Test
    @DisplayName("período informado ao entrar em tenant existente é ignorado — a licença é do cliente")
    void periodoIgnoradoEmTenantExistente() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"tenantId":"%s","nome":"Ana","email":"ana@clinicaA.local",
                                 "senha":"SenhaDaAna123","role":"USER","periodoAcesso":"UM_MES"}
                                """.formatted(tenantA.getId())))
                .andExpect(status().isCreated());

        Tenant recarregado = tenantRepository.findById(tenantA.getId()).orElseThrow();
        assertThat(recarregado.getPeriodoAcesso()).isEqualTo(PeriodoAcesso.INDETERMINADO);
        assertThat(recarregado.getAcessoExpiraEm()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────
    //  A rotina diária
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("a rotina desliga quem venceu e não toca em quem ainda tem prazo")
    void rotinaDesligaSomenteOsVencidos() {
        vencerAcesso(tenantA);
        prazoFuturo(tenantB);

        manutencaoJob.inativarAcessosExpirados();

        assertThat(tenantRepository.findById(tenantA.getId()).orElseThrow().getAtivo()).isFalse();
        assertThat(tenantRepository.findById(tenantB.getId()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    @DisplayName("a rotina nunca desliga o tenant do superadmin, mesmo com prazo vencido")
    void rotinaPoupaOTenantRaiz() {
        Tenant raiz = superadmin.getTenant();
        vencerAcesso(raiz);

        manutencaoJob.inativarAcessosExpirados();

        assertThat(tenantRepository.findById(raiz.getId()).orElseThrow().getAtivo()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────
    //  O login não espera a rotina
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login de cliente vencido é barrado mesmo antes de a rotina rodar")
    void loginBarraVencidoAntesDaRotina() throws Exception {
        vencerAcesso(tenantA);      // continua ativo: a rotina ainda não passou

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(adminA.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isUnauthorized());

        // Prova que a migration 008 estendeu o CHECK: sem ela o INSERT estoura.
        assertThat(loginLogRepository.findAll())
                .anyMatch(l -> l.getMotivoFalha() != null
                        && "ACESSO_EXPIRADO".equals(l.getMotivoFalha().name()));
    }

    @Test
    @DisplayName("refresh de cliente vencido também cai — é a mesma porta")
    void refreshBarraVencido() throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(adminA.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refresh = objectMapper.readTree(corpo).get("refreshToken").asText();
        vencerAcesso(tenantA);

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("o superadmin entra mesmo com prazo vencido no tenant raiz")
    void superadminNaoEBarradoPeloPrazo() throws Exception {
        vencerAcesso(superadmin.getTenant());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(superadmin.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Renovação
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reativar sem renovar é recusado — seria desfeito pela rotina na madrugada seguinte")
    void reativarVencidoSemRenovarFalha() throws Exception {
        vencerAcesso(tenantA);
        desativar(tenantA);

        mockMvc.perform(patch("/tenants/{id}/ativo", tenantA.getId())
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .param("ativo", "true"))
                .andExpect(status().isConflict());

        assertThat(tenantRepository.findById(tenantA.getId()).orElseThrow().getAtivo()).isFalse();
    }

    @Test
    @DisplayName("renovar reconta a data a partir de hoje e reativa o cliente vencido")
    void renovarReativaERecontaOPrazo() throws Exception {
        vencerAcesso(tenantA);
        desativar(tenantA);

        mockMvc.perform(patch("/tenants/{id}/acesso", tenantA.getId())
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"periodoAcesso":"SEIS_MESES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.periodoAcesso").value("SEIS_MESES"))
                .andExpect(jsonPath("$.acessoExpirado").value(false));

        Tenant renovado = tenantRepository.findById(tenantA.getId()).orElseThrow();
        assertThat(renovado.getAcessoExpiraEm()).isAfter(Instant.now());

        // E o acesso volta na prática, não só no registro.
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(adminA.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("renovar para indeterminado zera a data")
    void renovarParaIndeterminadoZeraAData() throws Exception {
        prazoFuturo(tenantA);

        mockMvc.perform(patch("/tenants/{id}/acesso", tenantA.getId())
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"periodoAcesso":"INDETERMINADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acessoExpiraEm").doesNotExist());

        assertThat(tenantRepository.findById(tenantA.getId()).orElseThrow()
                .getAcessoExpiraEm()).isNull();
    }

    @Test
    @DisplayName("cliente desativado à mão, sem prazo vencido, não é reativado pela renovação")
    void renovarNaoDesfazDesativacaoManual() throws Exception {
        desativar(tenantA);         // sem prazo: INDETERMINADO

        mockMvc.perform(patch("/tenants/{id}/acesso", tenantA.getId())
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"periodoAcesso":"UM_ANO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    @DisplayName("renovar é exclusivo do superadmin")
    void renovarExigeSuperadmin() throws Exception {
        mockMvc.perform(patch("/tenants/{id}/acesso", tenantA.getId())
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"periodoAcesso":"UM_ANO"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Aviso de vencimento
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("o filtro Expirando traz só quem vence dentro da janela")
    void filtroExpirandoRestringeAJanela() throws Exception {
        expirarEm(tenantA, 3);
        expirarEm(tenantB, 90);

        mockMvc.perform(get("/tenants")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .param("ativo", "true")
                        .param("expirandoEmDias", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(tenantA.getId().toString()));
    }

    // ─────────────────────────────────────────────────────────────────

    private Tenant tenantDe(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
        return tenantRepository.findById(usuario.getTenant().getId()).orElseThrow();
    }

    /**
     * Grava um prazo já vencido. O período precisa acompanhar a data: o CHECK
     * {@code ck_tenant_acesso_coerente} recusa data com INDETERMINADO.
     */
    private void vencerAcesso(Tenant tenant) {
        expirarEm(tenant, -1);
    }

    private void prazoFuturo(Tenant tenant) {
        expirarEm(tenant, 30);
    }

    private void expirarEm(Tenant tenant, int dias) {
        Tenant gerenciado = tenantRepository.findById(tenant.getId()).orElseThrow();
        gerenciado.setPeriodoAcesso(PeriodoAcesso.UM_MES);
        gerenciado.setAcessoExpiraEm(Instant.now().plus(dias, ChronoUnit.DAYS));
        tenantRepository.save(gerenciado);
    }

    private void desativar(Tenant tenant) {
        Tenant gerenciado = tenantRepository.findById(tenant.getId()).orElseThrow();
        gerenciado.setAtivo(false);
        tenantRepository.save(gerenciado);
    }
}
