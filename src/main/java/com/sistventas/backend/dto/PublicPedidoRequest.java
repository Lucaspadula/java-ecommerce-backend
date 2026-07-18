package com.sistventas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PublicPedidoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String clienteNombre,

        @NotBlank(message = "El teléfono es obligatorio")
        String clienteTelefono,

        @NotEmpty(message = "El pedido debe tener al menos un item")
        @Valid
        List<PublicPedidoItemRequest> items,

        // Opcional: si no viene o no coincide con el cupón cargado por la
        // empresa, no se aplica descuento (ver CuponInvalidoException).
        String cuponCodigo,

        // Los 3 siguientes son opcionales y puramente informativos: NO hay
        // cálculo de costo de envío (Fase 1), solo se guardan en la Venta
        // resultante para que el dueño tenga el dato a mano al coordinar la
        // entrega por WhatsApp.
        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
        String direccionEnvio,

        @Size(max = 150, message = "La localidad no puede superar los 150 caracteres")
        String localidad,

        @Size(max = 20, message = "El código postal no puede superar los 20 caracteres")
        String codigoPostal
) {}
