package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarCatalogoConfigRequest;
import com.sistventas.backend.dto.ActualizarEstiloTextoCatalogoRequest;
import com.sistventas.backend.dto.ActualizarPerfilRequest;
import com.sistventas.backend.dto.ActualizarAparienciaRequest;
import com.sistventas.backend.dto.ActualizarDatosTiendaRequest;
import com.sistventas.backend.dto.ActualizarPromocionesRequest;
import com.sistventas.backend.dto.BannerImagenTiendaDto;
import com.sistventas.backend.dto.CambiarPasswordRequest;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.PerfilDto;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaBannerImagen;
import com.sistventas.backend.entity.Usuario;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.BannerImagenNoEncontradaException;
import com.sistventas.backend.exception.PasswordActualIncorrectaException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.SlugEnUsoException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.TiendaBannerImagenRepository;
import com.sistventas.backend.repository.UsuarioRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests de PerfilServiceImpl.actualizarTienda: persistencia de la
// personalización de tienda pública (tiendaFuente/tiendaTema con default,
// resto de campos vacío-como-null) y el conflicto de slug entre empresas.
@ExtendWith(MockitoExtension.class)
class PerfilServiceImplTest {

    private static final Long EMPRESA_ID = 10L;
    private static final Long USUARIO_ID = 1L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ImagenUploadValidator imagenUploadValidator;

    @Mock
    private TiendaBannerImagenRepository tiendaBannerImagenRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private PerfilServiceImpl perfilService;

    private final UserPrincipal principal = new UserPrincipal(USUARIO_ID, EMPRESA_ID, false, RolEmpresa.ADMIN);

    // --- PUT parciales por pantalla (nav R5/E6): cada uno toca SOLO sus campos ---

    @Test
    void actualizarAparienciaPersisteFuenteTemaYTextosDelHeroSinTocarOtrosCampos() {
        Empresa empresa = empresa();
        empresa.setSlug("original");
        empresa.setTiendaCuponCodigo("CUPON");
        empresa.setTiendaContactoEmail("a@b.com");
        prepararEmpresaYUsuario(empresa);

        PerfilDto resultado = perfilService.actualizarApariencia(
                new ActualizarAparienciaRequest(" Sub ", "Titulo", "Desc", "moderna", "oscuro"), principal);

        assertThat(empresa.getTiendaFuente()).isEqualTo("moderna");
        assertThat(empresa.getTiendaTema()).isEqualTo("oscuro");
        assertThat(empresa.getTiendaBannerTagline()).isEqualTo("Sub");
        assertThat(empresa.getTiendaBannerTitulo()).isEqualTo("Titulo");
        assertThat(empresa.getTiendaBannerDescripcion()).isEqualTo("Desc");
        assertThat(resultado.tiendaFuente()).isEqualTo("moderna");
        assertThat(empresa.getSlug()).isEqualTo("original");
        assertThat(empresa.getTiendaCuponCodigo()).isEqualTo("CUPON");
        assertThat(empresa.getTiendaContactoEmail()).isEqualTo("a@b.com");
    }

    @Test
    void actualizarAparienciaConFuenteYTemaNulosOVaciosCaeALosDefaults() {
        Empresa empresa = empresa();
        prepararEmpresaYUsuario(empresa);

        perfilService.actualizarApariencia(new ActualizarAparienciaRequest(null, "  ", null, "   ", ""), principal);

        assertThat(empresa.getTiendaFuente()).isEqualTo("clasica");
        assertThat(empresa.getTiendaTema()).isEqualTo("claro");
        assertThat(empresa.getTiendaBannerTitulo()).isNull();
    }

    @Test
    void actualizarPromocionesPersisteCuponYOfertaSinTocarOtrosCampos() {
        Empresa empresa = empresa();
        empresa.setSlug("original");
        empresa.setTiendaFuente("elegante");
        prepararEmpresaYUsuario(empresa);
        LocalDateTime fin = LocalDateTime.of(2030, 1, 1, 10, 0);

        PerfilDto resultado = perfilService.actualizarPromociones(
                new ActualizarPromocionesRequest(" BIEN10 ", new BigDecimal("10"), true, "HOY", "Envio gratis", fin), principal);

        assertThat(empresa.getTiendaCuponCodigo()).isEqualTo("BIEN10");
        assertThat(empresa.getTiendaCuponPorcentaje()).isEqualByComparingTo("10");
        assertThat(empresa.isTiendaOfertaActiva()).isTrue();
        assertThat(empresa.getTiendaOfertaEtiqueta()).isEqualTo("HOY");
        assertThat(empresa.getTiendaOfertaTexto()).isEqualTo("Envio gratis");
        assertThat(empresa.getTiendaOfertaFechaFin()).isEqualTo(fin);
        assertThat(resultado.tiendaCuponCodigo()).isEqualTo("BIEN10");
        assertThat(empresa.getSlug()).isEqualTo("original");
        assertThat(empresa.getTiendaFuente()).isEqualTo("elegante");
    }

    @Test
    void actualizarDatosPersisteSlugContactoYLegalesSinTocarOtrosCampos() {
        Empresa empresa = empresa();
        empresa.setTiendaTema("oscuro");
        empresa.setTiendaCuponCodigo("CUPON");
        prepararEmpresaYUsuario(empresa);
        when(empresaRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());

        PerfilDto resultado = perfilService.actualizarDatosTienda(datos("mi-tienda"), principal);

        assertThat(empresa.getSlug()).isEqualTo("mi-tienda");
        assertThat(empresa.isTiendaHabilitada()).isTrue();
        assertThat(empresa.getTiendaContactoWhatsapp()).isEqualTo("549351");
        assertThat(empresa.getTiendaRazonSocial()).isEqualTo("Yeshua SRL");
        assertThat(empresa.getTiendaSobreNosotros()).isEqualTo("Historia");
        assertThat(resultado.empresaSlug()).isEqualTo("mi-tienda");
        assertThat(empresa.getTiendaTema()).isEqualTo("oscuro");
        assertThat(empresa.getTiendaCuponCodigo()).isEqualTo("CUPON");
    }

    @Test
    void actualizarDatosConSlugYaUsadoPorOtraEmpresaLanzaSlugEnUsoException() {
        Empresa otraEmpresa = empresa();
        otraEmpresa.setId(999L);
        when(empresaRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(otraEmpresa));

        assertThatThrownBy(() -> perfilService.actualizarDatosTienda(datos("mi-tienda"), principal))
                .isInstanceOf(SlugEnUsoException.class);
    }

    @Test
    void actualizarDatosConSlugDeLaPropiaEmpresaNoLanzaConflicto() {
        Empresa empresa = empresa();
        prepararEmpresaYUsuario(empresa);
        // La misma empresa reguardando su propio slug no es conflicto.
        when(empresaRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(empresa));

        PerfilDto resultado = perfilService.actualizarDatosTienda(datos("mi-tienda"), principal);

        assertThat(resultado.empresaSlug()).isEqualTo("mi-tienda");
    }

    @Test
    void actualizarParcialesConMemberLanzaAccesoRestringidoAdmin() {
        UserPrincipal member = new UserPrincipal(USUARIO_ID, EMPRESA_ID, false, RolEmpresa.MEMBER);

        assertThatThrownBy(() -> perfilService.actualizarApariencia(
                new ActualizarAparienciaRequest(null, null, null, null, null), member))
                .isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void actualizarLogoEmpresaGuardaLaUrlGeneradaEnLaEmpresa() {
        Empresa empresa = empresa();
        prepararEmpresaYUsuario(empresa);
        MultipartFile file = mock(MultipartFile.class);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".jpg");

        PerfilDto resultado = perfilService.actualizarLogoEmpresa(file, principal);

        assertThat(empresa.getLogoUrl()).startsWith("/uploads/empresas/" + EMPRESA_ID + "-");
        assertThat(empresa.getLogoUrl()).endsWith(".jpg");
        assertThat(resultado.logoUrl()).isEqualTo(empresa.getLogoUrl());
    }

    @Test
    void actualizarCatalogoPortadaImagenGuardaLaUrlGeneradaEnLaEmpresa() {
        Empresa empresa = empresa();
        prepararEmpresaYUsuario(empresa);
        MultipartFile file = mock(MultipartFile.class);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".jpg");

        PerfilDto resultado = perfilService.actualizarCatalogoPortadaImagen(file, principal);

        assertThat(empresa.getCatalogoPortadaImagenUrl()).startsWith("/uploads/empresas/" + EMPRESA_ID + "-");
        assertThat(empresa.getCatalogoPortadaImagenUrl()).endsWith(".jpg");
        assertThat(resultado.catalogoPortadaImagenUrl()).isEqualTo(empresa.getCatalogoPortadaImagenUrl());
    }

    @Test
    void quitarCatalogoPortadaImagenLaDejaEnNull() {
        Empresa empresa = empresa();
        empresa.setCatalogoPortadaImagenUrl("/uploads/empresas/10-vieja.jpg");
        prepararEmpresaYUsuario(empresa);

        PerfilDto resultado = perfilService.quitarCatalogoPortadaImagen(principal);

        assertThat(empresa.getCatalogoPortadaImagenUrl()).isNull();
        assertThat(resultado.catalogoPortadaImagenUrl()).isNull();
    }

    @Test
    void actualizarCatalogoConfigGuardaLosTogglesYElTituloEnLaEmpresa() {
        Empresa empresa = empresa();
        prepararEmpresaYUsuario(empresa);
        ActualizarCatalogoConfigRequest request =
                new ActualizarCatalogoConfigRequest(false, "Catálogo Yeshua 2026", false, false, "#f7f3ec");

        PerfilDto resultado = perfilService.actualizarCatalogoConfig(request, principal);

        assertThat(empresa.isCatalogoMostrarLogo()).isFalse();
        assertThat(empresa.getCatalogoTituloPersonalizado()).isEqualTo("Catálogo Yeshua 2026");
        assertThat(empresa.isCatalogoMostrarDescripcion()).isFalse();
        assertThat(empresa.isCatalogoMostrarColores()).isFalse();
        assertThat(empresa.getCatalogoColorFondoProductos()).isEqualTo("#f7f3ec");
        assertThat(resultado.catalogoTituloPersonalizado()).isEqualTo("Catálogo Yeshua 2026");
        assertThat(resultado.catalogoColorFondoProductos()).isEqualTo("#f7f3ec");
    }

    @Test
    void actualizarCatalogoConfigConTituloEnBlancoLoGuardaComoNull() {
        Empresa empresa = empresa();
        empresa.setCatalogoTituloPersonalizado("Título viejo");
        prepararEmpresaYUsuario(empresa);
        ActualizarCatalogoConfigRequest request = new ActualizarCatalogoConfigRequest(true, "   ", true, true, "");

        perfilService.actualizarCatalogoConfig(request, principal);

        assertThat(empresa.getCatalogoTituloPersonalizado()).isNull();
        assertThat(empresa.getCatalogoColorFondoProductos()).isNull();
    }

    @Test
    void actualizarEstiloTextoCatalogoGuardaLos3CamposEnLaEmpresa() {
        Empresa empresa = empresa();
        prepararEmpresaYUsuario(empresa);
        ActualizarEstiloTextoCatalogoRequest request =
                new ActualizarEstiloTextoCatalogoRequest("times", 14, "#900000");

        PerfilDto resultado = perfilService.actualizarEstiloTextoCatalogo(request, principal);

        assertThat(empresa.getCatalogoTextoFuente()).isEqualTo("times");
        assertThat(empresa.getCatalogoTextoTamanio()).isEqualTo(14);
        assertThat(empresa.getCatalogoTextoColor()).isEqualTo("#900000");
        assertThat(resultado.catalogoTextoFuente()).isEqualTo("times");
        assertThat(resultado.catalogoTextoTamanio()).isEqualTo(14);
        assertThat(resultado.catalogoTextoColor()).isEqualTo("#900000");
    }

    @Test
    void actualizarEstiloTextoCatalogoConCamposEnBlancoLosGuardaComoNull() {
        Empresa empresa = empresa();
        empresa.setCatalogoTextoFuente("times");
        empresa.setCatalogoTextoColor("#900000");
        prepararEmpresaYUsuario(empresa);
        ActualizarEstiloTextoCatalogoRequest request = new ActualizarEstiloTextoCatalogoRequest("", null, "");

        perfilService.actualizarEstiloTextoCatalogo(request, principal);

        assertThat(empresa.getCatalogoTextoFuente()).isNull();
        assertThat(empresa.getCatalogoTextoTamanio()).isNull();
        assertThat(empresa.getCatalogoTextoColor()).isNull();
    }

    @Test
    void cambiarPasswordConPasswordActualCorrectaHasheaYGuardaLaNueva() {
        Usuario usuario = usuarioConPassword("hash-viejo");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("actual123", "hash-viejo")).thenReturn(true);
        when(passwordEncoder.encode("nueva12345")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        CambiarPasswordRequest request = new CambiarPasswordRequest("actual123", "nueva12345");

        MensajeResponse resultado = perfilService.cambiarPassword(request, principal);

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(resultado.mensaje()).isEqualTo("Contraseña actualizada correctamente");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarPasswordConPasswordActualIncorrectaLanzaExcepcionYNoGuardaNada() {
        Usuario usuario = usuarioConPassword("hash-viejo");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mal-puesta", "hash-viejo")).thenReturn(false);

        CambiarPasswordRequest request = new CambiarPasswordRequest("mal-puesta", "nueva12345");

        assertThatThrownBy(() -> perfilService.cambiarPassword(request, principal))
                .isInstanceOf(PasswordActualIncorrectaException.class);

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-viejo");
        verify(usuarioRepository, never()).save(any());
    }

    // --- obtener / actualizar ---

    @Test
    void obtenerDevuelveElPerfilDelUsuarioDelPrincipal() {
        Empresa empresa = empresa();
        prepararUsuario(empresa);

        PerfilDto resultado = perfilService.obtener(principal);

        assertThat(resultado.id()).isEqualTo(USUARIO_ID);
        assertThat(resultado.nombre()).isEqualTo("Lucas");
    }

    @Test
    void obtenerConUsuarioSinEmpresaDevuelveCamposDeEmpresaEnNull() {
        // Caso Super Admin: Usuario.empresa es null, el toDto tiene que caer
        // en la rama "empresa == null" para cada campo derivado sin NPE.
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Super");
        usuario.setEmail("super@test.com");
        usuario.setRolEmpresa(null);
        usuario.setEsSuperAdmin(true);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        PerfilDto resultado = perfilService.obtener(principal);

        assertThat(resultado.empresaNombre()).isNull();
        assertThat(resultado.logoUrl()).isNull();
        assertThat(resultado.empresaSlug()).isNull();
        assertThat(resultado.tiendaHabilitada()).isFalse();
        assertThat(resultado.tiendaFuente()).isNull();
    }

    @Test
    void actualizarCambiaElNombreYLoPersiste() {
        Empresa empresa = empresa();
        prepararUsuario(empresa);
        Usuario usuario = usuarioRepository.findById(USUARIO_ID).orElseThrow();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        PerfilDto resultado = perfilService.actualizar(new ActualizarPerfilRequest("Lucas Nuevo"), principal);

        assertThat(usuario.getNombre()).isEqualTo("Lucas Nuevo");
        assertThat(resultado.nombre()).isEqualTo("Lucas Nuevo");
        verify(usuarioRepository).save(usuario);
    }

    // --- adminEmpresaIdOrThrow (compartido por logo/tienda/banners) ---

    @Test
    void actualizarLogoEmpresaConPrincipalNuloLanzaSinEmpresaException() {
        assertThatThrownBy(() -> perfilService.actualizarLogoEmpresa(mock(MultipartFile.class), null))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void actualizarLogoEmpresaConSuperAdminSinEmpresaLanzaSinEmpresaException() {
        UserPrincipal superAdmin = new UserPrincipal(USUARIO_ID, null, true, null);

        assertThatThrownBy(() -> perfilService.actualizarLogoEmpresa(mock(MultipartFile.class), superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void actualizarLogoEmpresaConRolMemberLanzaAccesoRestringidoAdmin() {
        UserPrincipal member = new UserPrincipal(USUARIO_ID, EMPRESA_ID, false, RolEmpresa.MEMBER);

        assertThatThrownBy(() -> perfilService.actualizarLogoEmpresa(mock(MultipartFile.class), member))
                .isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void actualizarLogoEmpresaConFalloDeIOLanzaUncheckedIOException() throws IOException {
        // No hace falta prepararEmpresaYUsuario: transferTo() explota antes
        // de llegar a empresaRepository.getReferenceById/save.
        MultipartFile file = mock(MultipartFile.class);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".jpg");
        doThrow(new IOException("disco lleno")).when(file).transferTo(any(java.nio.file.Path.class));

        assertThatThrownBy(() -> perfilService.actualizarLogoEmpresa(file, principal))
                .isInstanceOf(java.io.UncheckedIOException.class);
    }

    // --- listarBannerImagenes / agregarBannerImagen / eliminarBannerImagen / actualizarProductoBannerImagen ---

    @Test
    void listarBannerImagenesDevuelveLasDeLaEmpresaMapeadasAlDto() {
        TiendaBannerImagen imagen = bannerImagen(5L, "HERO", "/uploads/empresas/foo.jpg", null);
        when(tiendaBannerImagenRepository.findByEmpresaIdOrderByOrden(EMPRESA_ID)).thenReturn(List.of(imagen));

        List<BannerImagenTiendaDto> resultado = perfilService.listarBannerImagenes(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(5L);
        assertThat(resultado.get(0).imagenUrl()).isEqualTo("/uploads/empresas/foo.jpg");
    }

    @Test
    void agregarBannerImagenEsSiempreHeroYOrdenSaleDeLaCantidadActual() {
        MultipartFile file = mock(MultipartFile.class);
        when(tiendaBannerImagenRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "HERO")).thenReturn(2L);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".png");
        when(tiendaBannerImagenRepository.save(any(TiendaBannerImagen.class))).thenAnswer(inv -> inv.getArgument(0));

        BannerImagenTiendaDto resultado = perfilService.agregarBannerImagen(file, null, principal);

        assertThat(resultado.tipo()).isEqualTo("HERO");
        assertThat(resultado.orden()).isEqualTo(2);
        assertThat(resultado.productoId()).isNull();
    }

    @Test
    void agregarBannerImagenHeroEnElMaximoLanzaAccionNoPermitida() {
        when(tiendaBannerImagenRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "HERO")).thenReturn(6L);

        assertThatThrownBy(() -> perfilService.agregarBannerImagen(mock(MultipartFile.class), null, principal))
                .isInstanceOf(AccionNoPermitidaException.class)
                .hasMessageContaining("Máximo 6 imágenes en el banner");
    }

    @Test
    void agregarBannerImagenConProductoIdDeOtraEmpresaLanzaProductoNoEncontrado() {
        when(tiendaBannerImagenRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "HERO")).thenReturn(0L);
        when(productoRepository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.agregarBannerImagen(mock(MultipartFile.class), 99L, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    @Test
    void agregarBannerImagenConProductoIdValidoLoAsociaALaImagen() {
        MultipartFile file = mock(MultipartFile.class);
        Producto producto = new Producto();
        producto.setId(7L);
        when(tiendaBannerImagenRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "HERO")).thenReturn(0L);
        when(productoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".png");
        when(tiendaBannerImagenRepository.save(any(TiendaBannerImagen.class))).thenAnswer(inv -> inv.getArgument(0));

        BannerImagenTiendaDto resultado = perfilService.agregarBannerImagen(file, 7L, principal);

        assertThat(resultado.productoId()).isEqualTo(7L);
    }

    @Test
    void agregarBannerImagenConFalloDeIOLanzaUncheckedIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(tiendaBannerImagenRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "HERO")).thenReturn(0L);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".png");
        doThrow(new IOException("disco lleno")).when(file).transferTo(any(java.nio.file.Path.class));

        assertThatThrownBy(() -> perfilService.agregarBannerImagen(file, null, principal))
                .isInstanceOf(java.io.UncheckedIOException.class);
    }

    @Test
    void eliminarBannerImagenInexistenteLanzaBannerImagenNoEncontrada() {
        when(tiendaBannerImagenRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.eliminarBannerImagen(5L, principal))
                .isInstanceOf(BannerImagenNoEncontradaException.class);
    }

    @Test
    void eliminarBannerImagenExistenteBorraElArchivoYLaFila() {
        TiendaBannerImagen imagen = bannerImagen(5L, "HERO", "/uploads/empresas/no-existe-en-disco.jpg", null);
        when(tiendaBannerImagenRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(imagen));

        perfilService.eliminarBannerImagen(5L, principal);

        verify(tiendaBannerImagenRepository).delete(imagen);
    }

    @Test
    void eliminarBannerImagenConUrlQueNoEmpiezaConElPrefijoNoIntentaBorrarArchivo() {
        // borrarArchivoBannerSiExiste corta antes del Files.deleteIfExists
        // si la URL no arranca con "/uploads/empresas/" (defensivo, no
        // debería pasar en la práctica) — no debe romper la eliminación.
        TiendaBannerImagen imagen = bannerImagen(6L, "HERO", "https://otro-host/imagen.jpg", null);
        when(tiendaBannerImagenRepository.findByIdAndEmpresaId(6L, EMPRESA_ID)).thenReturn(Optional.of(imagen));

        perfilService.eliminarBannerImagen(6L, principal);

        verify(tiendaBannerImagenRepository).delete(imagen);
    }

    @Test
    void eliminarBannerImagenConUrlNulaNoIntentaBorrarArchivo() {
        TiendaBannerImagen imagen = bannerImagen(7L, "HERO", null, null);
        when(tiendaBannerImagenRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(imagen));

        perfilService.eliminarBannerImagen(7L, principal);

        verify(tiendaBannerImagenRepository).delete(imagen);
    }

    @Test
    void actualizarProductoBannerImagenInexistenteLanzaBannerImagenNoEncontrada() {
        when(tiendaBannerImagenRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.actualizarProductoBannerImagen(5L, 7L, principal))
                .isInstanceOf(BannerImagenNoEncontradaException.class);
    }

    @Test
    void actualizarProductoBannerImagenConProductoIdNuloLoDesasocia() {
        TiendaBannerImagen imagen = bannerImagen(5L, "HERO", "/uploads/empresas/foo.jpg", 3L);
        when(tiendaBannerImagenRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(imagen));
        when(tiendaBannerImagenRepository.save(any(TiendaBannerImagen.class))).thenAnswer(inv -> inv.getArgument(0));

        BannerImagenTiendaDto resultado = perfilService.actualizarProductoBannerImagen(5L, null, principal);

        assertThat(resultado.productoId()).isNull();
    }

    @Test
    void actualizarProductoBannerImagenConProductoIdValidoLoAsocia() {
        TiendaBannerImagen imagen = bannerImagen(5L, "HERO", "/uploads/empresas/foo.jpg", null);
        Producto producto = new Producto();
        producto.setId(9L);
        when(tiendaBannerImagenRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(imagen));
        when(productoRepository.findByIdAndEmpresaId(9L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(tiendaBannerImagenRepository.save(any(TiendaBannerImagen.class))).thenAnswer(inv -> inv.getArgument(0));

        BannerImagenTiendaDto resultado = perfilService.actualizarProductoBannerImagen(5L, 9L, principal);

        assertThat(resultado.productoId()).isEqualTo(9L);
    }

    private TiendaBannerImagen bannerImagen(Long id, String tipo, String imagenUrl, Long productoId) {
        TiendaBannerImagen imagen = new TiendaBannerImagen();
        imagen.setId(id);
        imagen.setEmpresaId(EMPRESA_ID);
        imagen.setTipo(tipo);
        imagen.setImagenUrl(imagenUrl);
        imagen.setProductoId(productoId);
        return imagen;
    }

    private Usuario usuarioConPassword(String passwordHash) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Lucas");
        usuario.setEmail("lucas@test.com");
        usuario.setRolEmpresa(RolEmpresa.ADMIN);
        usuario.setPasswordHash(passwordHash);
        return usuario;
    }

    // Versión liviana de prepararEmpresaYUsuario: solo stubea la búsqueda del
    // usuario (buscarUsuario), sin los stubs de empresaRepository que
    // Mockito marca como "unnecessary stubbing" en tests que no llegan a
    // tocar getReferenceById/save (ej. obtener/actualizar, que no pasan por
    // adminEmpresaIdOrThrow).
    private void prepararUsuario(Empresa empresa) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Lucas");
        usuario.setEmail("lucas@test.com");
        usuario.setRolEmpresa(RolEmpresa.ADMIN);
        usuario.setEmpresa(empresa);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
    }

    private void prepararEmpresaYUsuario(Empresa empresa) {
        when(empresaRepository.getReferenceById(EMPRESA_ID)).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresa);

        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Lucas");
        usuario.setEmail("lucas@test.com");
        usuario.setRolEmpresa(RolEmpresa.ADMIN);
        usuario.setEmpresa(empresa);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
    }

    private Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setId(EMPRESA_ID);
        empresa.setNombre("Mi Empresa");
        return empresa;
    }

    private ActualizarDatosTiendaRequest datos(String slug) {
        return new ActualizarDatosTiendaRequest(slug, true, "549351", "@ig", "a@b.com", "Yeshua SRL", "20-1", "Calle 1", "Historia");
    }
}
