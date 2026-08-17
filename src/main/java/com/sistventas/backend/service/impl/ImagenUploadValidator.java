package com.sistventas.backend.service.impl;

import com.sistventas.backend.exception.ArchivoInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// Helper compartido por todos los uploads de imagen "comunes" del proyecto
// (producto, ítem de venta, logo/banner de empresa): centraliza la validación
// que antes estaba duplicada (casi idéntica) en ProductoServiceImpl,
// VentaServiceImpl y PerfilServiceImpl.
//
// A diferencia de esa versión vieja, acá la extensión con la que se guarda el
// archivo en disco SALE de una whitelist cerrada de Content-Type, nunca del
// nombre de archivo que manda el cliente. Ese era el hueco de seguridad: un
// nombre de archivo tipo "foto.html" con un Content-Type spoofeado
// "image/jpeg" terminaba guardado como "algo.html" y servido públicamente
// desde /uploads/**, ejecutándose como HTML real en el navegador de
// cualquiera que lo abriera (Stored XSS). Mismo criterio que ya usa
// TiendaCategoriaServiceImpl.validarImagenYObtenerExtension, pero con una
// whitelist más amplia porque estas fotos (producto/venta/logo/banner) no
// necesitan transparencia como las de categoría.
@Component
public class ImagenUploadValidator {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private static final Map<String, String> CONTENT_TYPE_A_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    public String validarYObtenerExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo es obligatorio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ArchivoInvalidoException("La imagen no puede superar los 5MB");
        }
        // Map.of(...) es un Map inmutable que no acepta clave null: un
        // Content-Type ausente (algunos clientes no lo mandan) reventaba acá
        // con NullPointerException en vez de caer en el 400 de negocio
        // ArchivoInvalidoException de abajo.
        String contentType = file.getContentType();
        String extension = contentType != null ? CONTENT_TYPE_A_EXTENSION.get(contentType) : null;
        if (extension == null) {
            throw new ArchivoInvalidoException("La imagen debe ser JPG, PNG, WEBP o GIF");
        }
        return extension;
    }
}
