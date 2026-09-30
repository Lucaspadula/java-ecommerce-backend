package com.sistventas.backend.security;

import com.sistventas.backend.exception.TokenGoogleInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Verifica un ID token de Google Identity Services contra el endpoint
 * oficial de Google (sin agregar la dependencia google-api-client). Google
 * valida firma y expiración del lado suyo; acá solo confirmamos que la
 * respuesta sea 200, que el token haya sido emitido para esta aplicación
 * (aud) y que el email esté verificado.
 *
 * Extraído de AuthServiceImpl (login del panel/empresa) para que
 * PublicTiendaServiceImpl (login de cuentas de cliente) reuse la MISMA
 * validación en vez de duplicarla — los dos flujos de Google Sign-In del
 * sistema comparten este único punto de verdad.
 */
@Component
public class GoogleTokenVerifier {

    private final RestClient restClient;
    private final String googleClientId;

    public GoogleTokenVerifier(
            RestClient.Builder restClientBuilder,
            @Value("${sistventas.google.client-id}") String googleClientId) {
        this.restClient = restClientBuilder.build();
        this.googleClientId = googleClientId;
    }

    public GoogleTokenInfo verificar(String idToken) {
        Map<String, String> claims;
        try {
            claims = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={idToken}", idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, String>>() { });
        } catch (RestClientException ex) {
            throw new TokenGoogleInvalidoException();
        }

        if (claims == null || !googleClientId.equals(claims.get("aud"))) {
            throw new TokenGoogleInvalidoException();
        }

        if (!"true".equals(claims.get("email_verified"))) {
            throw new TokenGoogleInvalidoException();
        }

        String email = claims.get("email");
        String sub = claims.get("sub");
        if (email == null || sub == null) {
            throw new TokenGoogleInvalidoException();
        }

        return new GoogleTokenInfo(email, claims.get("name"), sub);
    }

    public record GoogleTokenInfo(String email, String nombre, String sub) { }
}
