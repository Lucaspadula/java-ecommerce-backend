package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarPerfilRequest;
import com.sistventas.backend.dto.CambiarPasswordRequest;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.PerfilDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.PasswordActualIncorrectaException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.PerfilService;
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

// Tests del controller invocando los métodos directamente (sin MockMvc): lo
// que interesa acá es que PerfilController delegue en el service con los
// parámetros correctos y arme el ResponseEntity con el status HTTP esperado,
// no simular el dispatcher HTTP completo ni @AuthenticationPrincipal.
@ExtendWith(MockitoExtension.class)
class PerfilControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private PerfilService perfilService;

    @InjectMocks
    private PerfilController perfilController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void obtenerDevuelveOkConElPerfilDelService() {
        PerfilDto dto = perfilDto("Lucas");
        when(perfilService.obtener(principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = perfilController.obtener(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).obtener(principal);
    }

    @Test
    void actualizarDevuelveOkConElPerfilActualizado() {
        ActualizarPerfilRequest request = new ActualizarPerfilRequest("Lucas Padula");
        PerfilDto dto = perfilDto("Lucas Padula");
        when(perfilService.actualizar(request, principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = perfilController.actualizar(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).actualizar(request, principal);
    }

    @Test
    void cambiarPasswordDevuelveOkConElMensajeDelService() {
        CambiarPasswordRequest request = new CambiarPasswordRequest("actual123", "nueva12345");
        MensajeResponse response = new MensajeResponse("Contraseña actualizada correctamente");
        when(perfilService.cambiarPassword(request, principal)).thenReturn(response);

        ResponseEntity<MensajeResponse> respuesta = perfilController.cambiarPassword(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(response);
        verify(perfilService).cambiarPassword(request, principal);
    }

    @Test
    void cambiarPasswordConPasswordActualIncorrectaPropagaLaExcepcionDelService() {
        CambiarPasswordRequest request = new CambiarPasswordRequest("incorrecta", "nueva12345");
        when(perfilService.cambiarPassword(request, principal))
                .thenThrow(new PasswordActualIncorrectaException());

        assertThatThrownBy(() -> perfilController.cambiarPassword(request, principal))
                .isInstanceOf(PasswordActualIncorrectaException.class);
    }

    private PerfilDto perfilDto(String nombre) {
        return new PerfilDto(
                99L, nombre, "lucas@test.com", RolEmpresa.ADMIN, "Mi Empresa", false,
                null, "mi-empresa", true,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, false
        );
    }
}
