package com.sistventas.backend.config;

import com.sistventas.backend.entity.Usuario;
import com.sistventas.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Bootstrapea el primer Super Admin si todavía no existe ninguno con
 * esSuperAdmin = true. Corre una sola vez por arranque (idempotente: si ya
 * hay un super admin, no hace nada).
 *
 * Credenciales configurables vía sistventas.super-admin.email / .password
 * en application.yml. Los defaults de desarrollo son
 * admin@sistventas.com / ChangeMe123! — DEBEN cambiarse en producción
 * seteando las variables de entorno SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD.
 */
@Component
public class SuperAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String superAdminEmail;
    private final String superAdminPassword;

    public SuperAdminBootstrapRunner(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${sistventas.super-admin.email}") String superAdminEmail,
            @Value("${sistventas.super-admin.password}") String superAdminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByEsSuperAdminTrue()) {
            return;
        }

        Usuario superAdmin = new Usuario();
        superAdmin.setNombre("Super Admin");
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setPasswordHash(passwordEncoder.encode(superAdminPassword));
        superAdmin.setEsSuperAdmin(true);
        superAdmin.setActivo(true);
        superAdmin.setFechaAlta(LocalDateTime.now());
        usuarioRepository.save(superAdmin);

        log.warn("Super Admin bootstrapeado con email '{}'. Si esto es un ambiente productivo, " +
                "CAMBIÁ la password por defecto ya mismo (variables SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD).",
                superAdminEmail);
    }
}
