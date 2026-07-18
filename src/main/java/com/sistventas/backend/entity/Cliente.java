package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como FK plana (no @ManyToOne a Empresa), mismo criterio que
    // Producto: esta feature nunca necesita navegar de Cliente a Empresa, así
    // que evitamos el join/lazy loading innecesario. El scoping multiempresa
    // filtra por este campo en el repository, nunca confiando en un valor
    // que venga del cliente (HTTP).
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 190)
    private String email;

    @Column(length = 40)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }
}
