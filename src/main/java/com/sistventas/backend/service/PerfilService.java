package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarCatalogoConfigRequest;
import com.sistventas.backend.dto.ActualizarEstiloTextoCatalogoRequest;
import com.sistventas.backend.dto.ActualizarGeminiApiKeyRequest;
import com.sistventas.backend.dto.ActualizarPerfilRequest;
import com.sistventas.backend.dto.ActualizarTiendaRequest;
import com.sistventas.backend.dto.BannerImagenTiendaDto;
import com.sistventas.backend.dto.CambiarPasswordRequest;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.PerfilDto;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PerfilService {
    PerfilDto obtener(UserPrincipal principal);

    PerfilDto actualizar(ActualizarPerfilRequest request, UserPrincipal principal);

    MensajeResponse cambiarPassword(CambiarPasswordRequest request, UserPrincipal principal);

    PerfilDto actualizarLogoEmpresa(MultipartFile file, UserPrincipal principal);

    PerfilDto actualizarTienda(ActualizarTiendaRequest request, UserPrincipal principal);

    PerfilDto actualizarGeminiApiKey(ActualizarGeminiApiKeyRequest request, UserPrincipal principal);

    List<BannerImagenTiendaDto> listarBannerImagenes(UserPrincipal principal);

    BannerImagenTiendaDto agregarBannerImagen(MultipartFile file, String tipo, Long productoId, UserPrincipal principal);

    void eliminarBannerImagen(Long imagenId, UserPrincipal principal);

    // Permite cambiar o sacar el producto asociado a una imagen YA subida,
    // sin tener que resubirla. productoId null = desvincular.
    BannerImagenTiendaDto actualizarProductoBannerImagen(Long imagenId, Long productoId, UserPrincipal principal);

    // Fondo de la portada del catálogo en PDF (ver CatalogoServiceImpl.dibujarPortada) — opcional, sin
    // ella se usa el color oscuro fijo de siempre.
    PerfilDto actualizarCatalogoPortadaImagen(MultipartFile file, UserPrincipal principal);

    PerfilDto quitarCatalogoPortadaImagen(UserPrincipal principal);

    // Toggles de qué mostrar en el catálogo PDF (ver CatalogoServiceImpl):
    // logo en la portada, título personalizado, descripción y colores por
    // producto.
    PerfilDto actualizarCatalogoConfig(ActualizarCatalogoConfigRequest request, UserPrincipal principal);

    // Estilo GLOBAL (tipografía/tamaño/color) del texto de "Importante"/
    // "Cómo comprar" en el catálogo PDF — ver CatalogoServiceImpl.
    PerfilDto actualizarEstiloTextoCatalogo(ActualizarEstiloTextoCatalogoRequest request, UserPrincipal principal);
}
