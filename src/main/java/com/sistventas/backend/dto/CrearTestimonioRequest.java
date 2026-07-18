package com.sistventas.backend.dto;

import com.sistventas.backend.entity.CanalTestimonio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearTestimonioRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String clienteNombre,

        @NotBlank(message = "El comentario es obligatorio")
        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comentario,

        // Opcional: el admin puede no elegir canal (ver TiendaTestimonio.canal).
        CanalTestimonio canal
) {}
