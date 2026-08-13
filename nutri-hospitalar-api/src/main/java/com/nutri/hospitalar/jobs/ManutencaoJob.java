package com.nutri.hospitalar.jobs;

import com.nutri.hospitalar.config.JwtProperties;
import com.nutri.hospitalar.loginlog.service.LoginLogService;
import com.nutri.hospitalar.refreshtoken.service.RefreshTokenService;
import com.nutri.hospitalar.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ManutencaoJob {

    private final LoginLogService loginLogService;
    private final RefreshTokenService refreshTokenService;
    private final TenantService tenantService;
    private final JwtProperties jwtProperties;

    @Value("${app.auditoria.retencao-meses:12}")
    private int retencaoMeses;

    @Scheduled(cron = "0 10 * * * *")     // a cada hora, aos 10 minutos
    public void fecharSessoesExpiradas() {
        Instant limite = Instant.now().minus(jwtProperties.getAccessTtl());
        int fechadas = loginLogService.fecharSessoesExpiradas(limite);
        if (fechadas > 0)
            log.info("Sessões expiradas fechadas: {}", fechadas);
    }

    @Scheduled(cron = "0 30 3 * * *")     // diariamente às 03h30
    public void expurgarRefreshTokens() {
        int removidos = refreshTokenService.expurgarExpirados(Instant.now());
        if (removidos > 0)
            log.info("Refresh tokens expirados removidos: {}", removidos);
    }

    /**
     * Desliga os clientes com o período de acesso vencido.
     *
     * <p>Só o total vai para o log — nome de cliente não entra. A janela entre
     * a virada do dia e esta rotina é coberta pelo próprio login, que barra o
     * prazo vencido sem depender daqui.
     */
    @Scheduled(cron = "0 15 3 * * *")     // diariamente às 03h15
    public void inativarAcessosExpirados() {
        int inativados = tenantService.inativarExpirados(Instant.now());
        if (inativados > 0)
            log.info("Clientes inativados por fim do período de acesso: {}", inativados);
    }

    @Scheduled(cron = "0 0 4 1 * *")      // dia 1 de cada mês, às 04h
    public void expurgarLogDeAcesso() {
        Instant limite = Instant.now().minus(retencaoMeses * 30L, ChronoUnit.DAYS);
        int removidos = loginLogService.expurgarAnterioresA(limite);
        log.info("Log de acesso expurgado: {} registros anteriores a {}", removidos, limite);
    }
}
