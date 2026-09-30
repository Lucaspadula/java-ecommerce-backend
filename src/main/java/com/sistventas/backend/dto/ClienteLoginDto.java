package com.sistventas.backend.dto;

// Datos del cliente que viajan junto al JWT en la respuesta de login — nunca
// passwordHash/googleSub. Mismo criterio que UsuarioLoginDto para el panel.
// telefono viaja para poder autocompletar el checkout sin pedirlo de nuevo
// (hallazgo del dueño, 2026-09: el checkout no precargaba nada del cliente
// logueado).
public record ClienteLoginDto(
        Long id,
        String nombre,
        String email,
        String telefono
) {}
