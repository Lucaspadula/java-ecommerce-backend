package com.sistventas.backend.controller;

import com.sistventas.backend.dto.GoogleLoginRequest;
import com.sistventas.backend.dto.GoogleRegistroEmpresaRequest;
import com.sistventas.backend.dto.GoogleUsuarioNoEncontradoDto;
import com.sistventas.backend.dto.LoginRequest;
import com.sistventas.backend.dto.LoginResponse;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.RegistroEmpresaRequest;
import com.sistventas.backend.dto.UsuarioLoginDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.CredencialesInvalidasException;
import com.sistventas.backend.exception.EmailYaRegistradoException;
import com.sistventas.backend.exception.UsuarioGoogleNoRegistradoException;
import com.sistventas.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc): no
// hace falta simular @AuthenticationPrincipal porque ningún endpoint de auth
// lo usa (son públicos por definición). Lo que interesa es que
// AuthController delegue en AuthService con los parámetros correctos y arme
// el ResponseEntity/status esperado, incluido el manejo especial de
// UsuarioGoogleNoRegistradoException que el controller atrapa directamente
// en vez de dejarla subir al GlobalExceptionHandler.
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registrarEmpresaDevuelveCreatedConElMensajeDelService() {
        RegistroEmpresaRequest request = new RegistroEmpresaRequest("Mi Empresa", "Lucas", "lucas@test.com", "password123");
        MensajeResponse response = new MensajeResponse("Empresa registrada correctamente");
        when(authService.registrarEmpresa(request)).thenReturn(response);

        ResponseEntity<MensajeResponse> respuesta = authController.registrarEmpresa(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(response);
        verify(authService).registrarEmpresa(request);
    }

    @Test
    void loginDevuelveOkConElTokenDelService() {
        LoginRequest request = new LoginRequest("lucas@test.com", "password123");
        LoginResponse response = new LoginResponse("jwt-token", usuarioLoginDto());
        when(authService.login(request)).thenReturn(response);

        ResponseEntity<LoginResponse> respuesta = authController.login(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(response);
        verify(authService).login(request);
    }

    @Test
    void loginConCredencialesInvalidasPropagaLaExcepcionDelService() {
        LoginRequest request = new LoginRequest("lucas@test.com", "incorrecta");
        when(authService.login(request)).thenThrow(new CredencialesInvalidasException());

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void loginConGoogleDevuelveOkConElTokenDelService() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token-valido");
        LoginResponse response = new LoginResponse("jwt-token", usuarioLoginDto());
        when(authService.loginConGoogle(request)).thenReturn(response);

        ResponseEntity<?> respuesta = authController.loginConGoogle(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(response);
        verify(authService).loginConGoogle(request);
    }

    // Caso particular: el controller atrapa esta excepción directamente
    // (no la deja llegar al GlobalExceptionHandler) para devolver 404 con un
    // DTO propio que incluye el email y nombre sugeridos por Google.
    @Test
    void loginConGoogleSinUsuarioRegistradoDevuelveNotFoundConDatosSugeridos() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token-sin-cuenta");
        when(authService.loginConGoogle(request))
                .thenThrow(new UsuarioGoogleNoRegistradoException("nuevo@test.com", "Nuevo Usuario"));

        ResponseEntity<?> respuesta = authController.loginConGoogle(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isInstanceOf(GoogleUsuarioNoEncontradoDto.class);
        GoogleUsuarioNoEncontradoDto body = (GoogleUsuarioNoEncontradoDto) respuesta.getBody();
        assertThat(body.email()).isEqualTo("nuevo@test.com");
        assertThat(body.nombreSugerido()).isEqualTo("Nuevo Usuario");
    }

    @Test
    void registrarEmpresaConGoogleDevuelveCreatedConElMensajeDelService() {
        GoogleRegistroEmpresaRequest request = new GoogleRegistroEmpresaRequest("id-token-valido", "Mi Empresa");
        MensajeResponse response = new MensajeResponse("Empresa registrada correctamente");
        when(authService.registrarEmpresaConGoogle(request)).thenReturn(response);

        ResponseEntity<MensajeResponse> respuesta = authController.registrarEmpresaConGoogle(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(response);
        verify(authService).registrarEmpresaConGoogle(request);
    }

    @Test
    void registrarEmpresaConEmailYaRegistradoPropagaLaExcepcionDelService() {
        RegistroEmpresaRequest request = new RegistroEmpresaRequest("Mi Empresa", "Lucas", "lucas@test.com", "password123");
        when(authService.registrarEmpresa(request)).thenThrow(new EmailYaRegistradoException());

        assertThatThrownBy(() -> authController.registrarEmpresa(request))
                .isInstanceOf(EmailYaRegistradoException.class);
    }

    private UsuarioLoginDto usuarioLoginDto() {
        return new UsuarioLoginDto(1L, "Lucas", "lucas@test.com", false, RolEmpresa.ADMIN, 10L, "Mi Empresa", null);
    }
}
