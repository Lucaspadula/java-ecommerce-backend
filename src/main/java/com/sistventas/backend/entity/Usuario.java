package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 190, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_empresa", length = 20)
    private RolEmpresa rolEmpresa;

    @Column(name = "es_super_admin", nullable = false)
    private boolean esSuperAdmin = false;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public RolEmpresa getRolEmpresa() { return rolEmpresa; }
    public void setRolEmpresa(RolEmpresa rolEmpresa) { this.rolEmpresa = rolEmpresa; }

    public boolean isEsSuperAdmin() { return esSuperAdmin; }
    public void setEsSuperAdmin(boolean esSuperAdmin) { this.esSuperAdmin = esSuperAdmin; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }
}
