package com.sistventas.backend.dto;

public record LoginResponse(String token, UsuarioLoginDto usuario) {}
