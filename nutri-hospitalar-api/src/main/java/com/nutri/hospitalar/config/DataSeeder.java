package com.nutri.hospitalar.config;

import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.repository.TenantRepository;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.enums.Role;
import com.nutri.hospitalar.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.tenant-nome}")
    private String tenantNome;

    @Value("${app.superadmin.nome}")
    private String nome;

    @Value("${app.superadmin.email}")
    private String email;

    @Value("${app.superadmin.senha}")
    private String senha;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByRole(Role.SUPERADMIN)) {
            log.debug("Superadmin já existe — seeding ignorado");
            return;
        }

        validarConfiguracao();

        Tenant tenantRaiz = tenantRepository.save(new Tenant(tenantNome));

        Usuario superadmin = new Usuario();
        superadmin.setTenant(tenantRaiz);
        superadmin.setNome(nome);
        superadmin.setEmail(email.trim().toLowerCase());
        superadmin.setSenha(passwordEncoder.encode(senha));
        superadmin.setRole(Role.SUPERADMIN);
        usuarioRepository.save(superadmin);

        log.info("Tenant raiz '{}' e superadmin criados na primeira subida", tenantNome);
    }

    private void validarConfiguracao() {
        exigir(nome, "SUPERADMIN_NAME");
        exigir(email, "SUPERADMIN_EMAIL");
        exigir(senha, "SUPERADMIN_PASSWORD");

        if (senha.length() < 10)
            throw new IllegalStateException("SUPERADMIN_PASSWORD precisa de no mínimo 10 caracteres");
    }

    private void exigir(String valor, String variavel) {
        if (valor == null || valor.isBlank())
            throw new IllegalStateException("Variável de ambiente obrigatória não definida: " + variavel);
    }
}
