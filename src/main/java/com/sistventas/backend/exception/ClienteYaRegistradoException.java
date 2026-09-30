package com.sistventas.backend.exception;

// Cubre los 2 casos de conflicto al registrar una cuenta de cliente: el
// email ya pertenece a otra cuenta, o el teléfono ya tiene una cuenta
// creada (ambos casos con mensaje propio, ver PublicTiendaServiceImpl).
public class ClienteYaRegistradoException extends RuntimeException {
    public ClienteYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}
