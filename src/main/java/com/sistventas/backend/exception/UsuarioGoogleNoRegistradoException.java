package com.sistventas.backend.exception;

/**
 * Se lanza cuando un idToken de Google es válido pero no existe ningún
 * Usuario con ese email todavía. El controller la atrapa directamente
 * (no vía GlobalExceptionHandler) para devolver el email y el nombre
 * sugerido extraídos del token, que el frontend necesita para ofrecer
 * el alta de la cuenta.
 */
public class UsuarioGoogleNoRegistradoException extends RuntimeException {

    private final String email;
    private final String nombre;

    public UsuarioGoogleNoRegistradoException(String email, String nombre) {
        super("No existe una cuenta con el email " + email);
        this.email = email;
        this.nombre = nombre;
    }

    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
}
