package com.sistventas.backend.entity;

// Canal por el que llegó el testimonio (ej. el dueño lo copia de un chat de
// WhatsApp o de un comentario de Instagram). Opcional: el admin puede dejarlo
// sin elegir (ver TiendaTestimonio.canal, nullable) — no todo testimonio
// cargado a mano tiene un canal identificable. OTRO cubre cualquier canal
// que no sea uno de los dos con ícono propio en la tienda pública.
public enum CanalTestimonio {
    WHATSAPP,
    INSTAGRAM,
    OTRO
}
