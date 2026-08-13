package com.nutri.hospitalar.loginlog.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.loginlog.enums.MotivoFalha;
import com.nutri.hospitalar.loginlog.enums.TipoLogout;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "login_log")
@Getter
@Setter
@NoArgsConstructor
public class LoginLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "email_tentativa", length = 255)
    private String emailTentativa;

    @Column(name = "sucesso", nullable = false)
    private Boolean sucesso;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_falha", length = 30)
    private MotivoFalha motivoFalha;

    @Column(name = "data_login", nullable = false)
    private Instant dataLogin;

    @Column(name = "data_logout")
    private Instant dataLogout;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_logout", length = 20)
    private TipoLogout tipoLogout;

    @Column(name = "endereco_ip", length = 45)
    private String enderecoIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impersonado_por")
    private Usuario impersonadoPor;
}
