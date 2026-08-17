package com.sistventas.backend.dto;

// Sin @NotBlank a propósito: mandar apiKey vacío/null es la forma de BORRAR
// la key propia de la empresa (vuelve a usar el fallback global) — ver
// PerfilServiceImpl.actualizarGeminiApiKey.
public record ActualizarGeminiApiKeyRequest(String apiKey) { }
