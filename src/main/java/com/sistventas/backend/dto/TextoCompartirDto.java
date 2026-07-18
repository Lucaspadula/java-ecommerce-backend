package com.sistventas.backend.dto;

// telefonoCliente puede ser null si el cliente no lo cargó: el frontend lo
// usa para armar el link de WhatsApp directo a ese contacto si existe.
public record TextoCompartirDto(
        String texto,
        String telefonoCliente
) {}
