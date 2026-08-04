package com.sistventas.backend.service.impl;

import com.sistventas.backend.exception.ArchivoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Tests de ImagenUploadValidator: la whitelist cerrada de Content-Type (nunca
// el nombre de archivo del cliente) que evita el hueco de Stored XSS descrito
// en el comentario de la clase, más los límites de tamaño y archivo vacío.
@ExtendWith(MockitoExtension.class)
class ImagenUploadValidatorTest {

    private static final long UN_MB = 1024L * 1024L;

    private final ImagenUploadValidator validator = new ImagenUploadValidator();

    @Mock
    private MultipartFile file;

    @Test
    void archivoNuloLanzaArchivoInvalido() {
        assertThatThrownBy(() -> validator.validarYObtenerExtension(null))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessage("El archivo es obligatorio");
    }

    @Test
    void archivoVacioLanzaArchivoInvalido() {
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> validator.validarYObtenerExtension(file))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessage("El archivo es obligatorio");
    }

    @Test
    void archivoQueSuperaLos5MbLanzaArchivoInvalido() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6 * UN_MB);

        assertThatThrownBy(() -> validator.validarYObtenerExtension(file))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessage("La imagen no puede superar los 5MB");
    }

    @Test
    void archivoDeExactamente5MbNoLanzaPorTamano() {
        // Límite inclusive: el chequeo es "> MAX_FILE_SIZE", así que
        // exactamente 5MB tiene que pasar el filtro de tamaño.
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5 * UN_MB);
        when(file.getContentType()).thenReturn("image/jpeg");

        String extension = validator.validarYObtenerExtension(file);

        assertThat(extension).isEqualTo(".jpg");
    }

    @Test
    void contentTypeNoSoportadoLanzaArchivoInvalido() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(UN_MB);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(() -> validator.validarYObtenerExtension(file))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessage("La imagen debe ser JPG, PNG, WEBP o GIF");
    }

    @Test
    void contentTypeNuloLanzaArchivoInvalido() {
        // Un nombre de archivo tipo "foto.html" con Content-Type nulo/ausente
        // no puede colarse: la extensión SIEMPRE sale de la whitelist, nunca
        // del nombre de archivo (ver comentario de la clase).
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(UN_MB);
        when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> validator.validarYObtenerExtension(file))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessage("La imagen debe ser JPG, PNG, WEBP o GIF");
    }

    @Test
    void contentTypeJpegSpoofeadoConNombreHtmlDevuelveExtensionJpgIgualmente() {
        // El caso puntual que motivó esta clase (ver comentario): un
        // Content-Type "image/jpeg" siempre resuelve a ".jpg" sin importar
        // qué nombre de archivo mande el cliente.
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(UN_MB);
        when(file.getContentType()).thenReturn("image/jpeg");

        String extension = validator.validarYObtenerExtension(file);

        assertThat(extension).isEqualTo(".jpg");
    }

    @Test
    void contentTypePngDevuelveExtensionPng() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(UN_MB);
        when(file.getContentType()).thenReturn("image/png");

        assertThat(validator.validarYObtenerExtension(file)).isEqualTo(".png");
    }

    @Test
    void contentTypeWebpDevuelveExtensionWebp() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(UN_MB);
        when(file.getContentType()).thenReturn("image/webp");

        assertThat(validator.validarYObtenerExtension(file)).isEqualTo(".webp");
    }

    @Test
    void contentTypeGifDevuelveExtensionGif() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(UN_MB);
        when(file.getContentType()).thenReturn("image/gif");

        assertThat(validator.validarYObtenerExtension(file)).isEqualTo(".gif");
    }
}
