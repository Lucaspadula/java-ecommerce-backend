package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.GoogleLoginRequest;
import com.sistventas.backend.dto.GoogleRegistroEmpresaRequest;
import com.sistventas.backend.dto.LoginRequest;
import com.sistventas.backend.dto.LoginResponse;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.RegistroEmpresaRequest;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.LicenciaEstado;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Usuario;
import com.sistventas.backend.exception.CredencialesInvalidasException;
import com.sistventas.backend.exception.CuentaDeshabilitadaException;
import com.sistventas.backend.exception.EmailYaRegistradoException;
import com.sistventas.backend.exception.LicenciaNoActivaException;
import com.sistventas.backend.exception.TokenGoogleInvalidoException;
import com.sistventas.backend.exception.UsuarioGoogleNoRegistradoException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.UsuarioRepository;
import com.sistventas.backend.security.GoogleTokenVerifier;
import com.sistventas.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

// Tests de AuthServiceImpl: login (con las 3 reglas de acceso: cuenta activa,
// licencia de empresa, super admin exento de la licencia), registro de
// empresa (hasheo de password, email duplicado), token JWT resultante y los
// flujos de Google (login/registro contra el endpoint tokeninfo de Google).
// JwtService se usa real (no mockeado): es una clase simple sin dependencias
// externas más allá de la librería jjwt, así que probarla contra la
// implementación real confirma que el token generado trae los claims
// esperados en vez de solo verificar que "se llamó a generateToken()".
// El RestClient que pega a oauth2.googleapis.com/tokeninfo se simula con
// MockRestServiceServer, atado al RestClient.Builder que ahora recibe el
// constructor de AuthServiceImpl (antes instanciaba su propio RestClient
// internamente vía RestClient.create(), lo que hacía imposible mockearlo sin
// pegarle a la red real): nunca sale una request real a Google en estos tests.
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String JWT_SECRET_TEST =
            "test-secret-key-for-jwt-signing-must-be-long-enough-1234567890";
    private static final String GOOGLE_CLIENT_ID = "google-client-id-test";

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtService jwtService;
    private AuthServiceImpl authService;
    private MockRestServiceServer mockGoogleServer;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(JWT_SECRET_TEST, 3_600_000L);
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockGoogleServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        GoogleTokenVerifier googleTokenVerifier = new GoogleTokenVerifier(restClientBuilder, GOOGLE_CLIENT_ID);
        authService = new AuthServiceImpl(
                empresaRepository, usuarioRepository, passwordEncoder, jwtService, googleTokenVerifier);
    }

    private String tokenInfoJson(String aud, String emailVerified, String email, String sub, String nombre) {
        return "{"
                + "\"aud\":\"" + aud + "\","
                + "\"email_verified\":\"" + emailVerified + "\","
                + "\"email\":\"" + email + "\","
                + "\"sub\":\"" + sub + "\","
                + "\"name\":\"" + nombre + "\""
                + "}";
    }

    // --- login ---

    @Test
    void loginExitosoConEmpresaActivaDevuelveTokenConClaimsYDatosDeUsuario() {
        Empresa empresa = empresa(1L, LicenciaEstado.ACTIVA);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, true);

        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("ana@test.com", "1234"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.usuario().id()).isEqualTo(10L);
        assertThat(response.usuario().email()).isEqualTo("ana@test.com");
        assertThat(response.usuario().empresaId()).isEqualTo(1L);
        assertThat(response.usuario().rolEmpresa()).isEqualTo(RolEmpresa.ADMIN);
        assertThat(response.usuario().esSuperAdmin()).isFalse();

        Claims claims = jwtService.parseClaims(response.token());
        assertThat(claims.getSubject()).isEqualTo("10");
        assertThat(claims.get("email", String.class)).isEqualTo("ana@test.com");
        assertThat(claims.get("rolEmpresa", String.class)).isEqualTo("ADMIN");
        assertThat(((Number) claims.get("empresaId")).longValue()).isEqualTo(1L);
        assertThat(claims.get("esSuperAdmin", Boolean.class)).isFalse();
    }

    @Test
    void loginConEmailInexistenteLanzaCredencialesInvalidas() {
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@test.com", "1234")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void loginConPasswordIncorrectaLanzaLaMismaExcepcionQueEmailInexistente() {
        // El mensaje no debe distinguir "no existe el email" de "password
        // incorrecta": ambos casos caen en el mismo orElseThrow con el mismo
        // texto, evitando darle a un atacante pistas sobre qué emails están
        // registrados en el sistema.
        Empresa empresa = empresa(1L, LicenciaEstado.ACTIVA);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, true);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.com", "mala")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void loginConUsuarioSinPasswordHashLanzaCredencialesInvalidasSinNpe() {
        // Caso de un usuario dado de alta solo por Google (passwordHash
        // null): el filter de login() chequea u.getPasswordHash() != null
        // antes de llamar a encoder.matches(), así que no debe reventar con
        // NullPointerException al intentar loguearse con password.
        Empresa empresa = empresa(1L, LicenciaEstado.ACTIVA);
        Usuario usuario = usuario(10L, "google@test.com", null, empresa, RolEmpresa.ADMIN, false, true);
        when(usuarioRepository.findByEmail("google@test.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("google@test.com", "1234")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void loginConUsuarioInactivoLanzaCuentaDeshabilitada() {
        Empresa empresa = empresa(1L, LicenciaEstado.ACTIVA);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, false);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.com", "1234")))
                .isInstanceOf(CuentaDeshabilitadaException.class);
    }

    @Test
    void loginConEmpresaPendienteLanzaLicenciaNoActiva() {
        Empresa empresa = empresa(1L, LicenciaEstado.PENDIENTE);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, true);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.com", "1234")))
                .isInstanceOf(LicenciaNoActivaException.class)
                .hasMessage("Tu empresa está pendiente de aprobación.");
    }

    @Test
    void loginConEmpresaSuspendidaLanzaLicenciaNoActiva() {
        Empresa empresa = empresa(1L, LicenciaEstado.SUSPENDIDA);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, true);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.com", "1234")))
                .isInstanceOf(LicenciaNoActivaException.class)
                .hasMessage("La licencia de tu empresa está suspendida.");
    }

    @Test
    void loginConEmpresaVencidaLanzaLicenciaNoActiva() {
        Empresa empresa = empresa(1L, LicenciaEstado.VENCIDA);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, true);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.com", "1234")))
                .isInstanceOf(LicenciaNoActivaException.class)
                .hasMessage("La licencia de tu empresa venció.");
    }

    @Test
    void loginDeSuperAdminIgnoraLicenciaDeEmpresaNoActiva() {
        // La validación de licencia vive en el "else" de "si NO es super
        // admin" (validarAccesoUsuario): un super admin cuya empresa está
        // PENDIENTE/SUSPENDIDA/VENCIDA tiene que poder entrar igual.
        Empresa empresa = empresa(1L, LicenciaEstado.SUSPENDIDA);
        Usuario usuario = usuario(10L, "admin@test.com", "hash", empresa, RolEmpresa.ADMIN, true, true);
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("admin@test.com", "1234"));

        assertThat(response.usuario().esSuperAdmin()).isTrue();
    }

    // --- loginConGoogle ---

    @Test
    void loginConGoogleExitosoConUsuarioSinGoogleSubLoSeteaYGuarda() {
        Empresa empresa = empresa(1L, LicenciaEstado.ACTIVA);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, true);
        usuario.setGoogleSub(null);

        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        tokenInfoJson(GOOGLE_CLIENT_ID, "true", "ana@test.com", "sub-123", "Ana"),
                        APPLICATION_JSON));
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        LoginResponse response = authService.loginConGoogle(new GoogleLoginRequest("un-id-token"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.usuario().email()).isEqualTo("ana@test.com");
        assertThat(usuario.getGoogleSub()).isEqualTo("sub-123");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void loginConGoogleConUsuarioQueYaTieneGoogleSubNoLoVuelveAGuardar() {
        Empresa empresa = empresa(1L, LicenciaEstado.ACTIVA);
        Usuario usuario = usuario(10L, "ana@test.com", "hash", empresa, RolEmpresa.ADMIN, false, true);
        usuario.setGoogleSub("sub-ya-seteado");

        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        tokenInfoJson(GOOGLE_CLIENT_ID, "true", "ana@test.com", "sub-ya-seteado", "Ana"),
                        APPLICATION_JSON));
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));

        authService.loginConGoogle(new GoogleLoginRequest("un-id-token"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginConGoogleConEmailNoRegistradoLanzaUsuarioGoogleNoRegistradoConEmailYNombre() {
        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        tokenInfoJson(GOOGLE_CLIENT_ID, "true", "nueva@test.com", "sub-999", "Nueva Persona"),
                        APPLICATION_JSON));
        when(usuarioRepository.findByEmail("nueva@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginConGoogle(new GoogleLoginRequest("un-id-token")))
                .isInstanceOf(UsuarioGoogleNoRegistradoException.class)
                .satisfies(ex -> {
                    UsuarioGoogleNoRegistradoException typed = (UsuarioGoogleNoRegistradoException) ex;
                    assertThat(typed.getEmail()).isEqualTo("nueva@test.com");
                    assertThat(typed.getNombre()).isEqualTo("Nueva Persona");
                });
    }

    @Test
    void loginConGoogleConRespuestaDeGoogleConErrorDeServidorLanzaTokenGoogleInvalido() {
        // El endpoint de Google responde 500 (o cualquier error de red): se
        // envuelve como RestClientException y se traduce a la excepción de
        // negocio, sin filtrar detalles internos del cliente HTTP.
        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> authService.loginConGoogle(new GoogleLoginRequest("un-id-token")))
                .isInstanceOf(TokenGoogleInvalidoException.class);
    }

    @Test
    void loginConGoogleConAudienceQueNoCoincideLanzaTokenGoogleInvalido() {
        // El token fue emitido para OTRA aplicación (aud distinto al
        // configurado en sistventas.google.client-id): no se confía en él.
        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        tokenInfoJson("otra-app-client-id", "true", "ana@test.com", "sub-123", "Ana"),
                        APPLICATION_JSON));

        assertThatThrownBy(() -> authService.loginConGoogle(new GoogleLoginRequest("un-id-token")))
                .isInstanceOf(TokenGoogleInvalidoException.class);
    }

    @Test
    void loginConGoogleConEmailNoVerificadoLanzaTokenGoogleInvalido() {
        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        tokenInfoJson(GOOGLE_CLIENT_ID, "false", "ana@test.com", "sub-123", "Ana"),
                        APPLICATION_JSON));

        assertThatThrownBy(() -> authService.loginConGoogle(new GoogleLoginRequest("un-id-token")))
                .isInstanceOf(TokenGoogleInvalidoException.class);
    }

    @Test
    void loginConGoogleConClaimsSinEmailOSubLanzaTokenGoogleInvalido() {
        // JSON sin "email" ni "sub": el chequeo defensivo evita seguir
        // adelante con un GoogleTokenInfo a medio completar.
        String jsonSinEmailNiSub = "{\"aud\":\"" + GOOGLE_CLIENT_ID + "\",\"email_verified\":\"true\"}";
        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(jsonSinEmailNiSub, APPLICATION_JSON));

        assertThatThrownBy(() -> authService.loginConGoogle(new GoogleLoginRequest("un-id-token")))
                .isInstanceOf(TokenGoogleInvalidoException.class);
    }

    // --- registrarEmpresaConGoogle ---

    @Test
    void registrarEmpresaConGoogleConEmailLibreCreaEmpresaYUsuarioSinPasswordConGoogleSub() {
        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        tokenInfoJson(GOOGLE_CLIENT_ID, "true", "nueva@test.com", "sub-777", "Nueva Persona"),
                        APPLICATION_JSON));
        when(usuarioRepository.findByEmail("nueva@test.com")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa guardada = invocation.getArgument(0);
            guardada.setId(2L);
            return guardada;
        });

        GoogleRegistroEmpresaRequest request = new GoogleRegistroEmpresaRequest("un-id-token", "Mi Empresa Google");

        MensajeResponse response = authService.registrarEmpresaConGoogle(request);

        assertThat(response.mensaje()).contains("pendiente de aprobación");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();

        assertThat(guardado.getEmail()).isEqualTo("nueva@test.com");
        assertThat(guardado.getNombre()).isEqualTo("Nueva Persona");
        assertThat(guardado.getPasswordHash()).isNull();
        assertThat(guardado.getGoogleSub()).isEqualTo("sub-777");
        assertThat(guardado.getRolEmpresa()).isEqualTo(RolEmpresa.ADMIN);
        assertThat(guardado.isEsSuperAdmin()).isFalse();
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    void registrarEmpresaConGoogleConEmailYaRegistradoLanzaExcepcionYNoPersisteNada() {
        mockGoogleServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        tokenInfoJson(GOOGLE_CLIENT_ID, "true", "existente@test.com", "sub-1", "Alguien"),
                        APPLICATION_JSON));
        Usuario existente = usuario(1L, "existente@test.com", "hash", null, RolEmpresa.ADMIN, false, true);
        when(usuarioRepository.findByEmail("existente@test.com")).thenReturn(Optional.of(existente));

        GoogleRegistroEmpresaRequest request = new GoogleRegistroEmpresaRequest("un-id-token", "Otra Empresa");

        assertThatThrownBy(() -> authService.registrarEmpresaConGoogle(request))
                .isInstanceOf(EmailYaRegistradoException.class);

        verify(empresaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    // --- registrarEmpresa ---

    @Test
    void registrarEmpresaConEmailLibreCreaEmpresaYUsuarioConPasswordHasheado() {
        when(usuarioRepository.findByEmail("nueva@test.com")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa guardada = invocation.getArgument(0);
            guardada.setId(1L);
            return guardada;
        });
        when(passwordEncoder.encode("password123")).thenReturn("hash-seguro");

        RegistroEmpresaRequest request = new RegistroEmpresaRequest(
                "Mi Empresa", "Nueva Usuaria", "nueva@test.com", "password123");

        MensajeResponse response = authService.registrarEmpresa(request);

        assertThat(response.mensaje()).contains("pendiente de aprobación");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();

        // El password nunca se persiste en texto plano: lo que llega al save()
        // tiene que ser el resultado de encoder.encode(), no el original.
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(guardado.getPasswordHash()).isNotEqualTo("password123");
        assertThat(guardado.getRolEmpresa()).isEqualTo(RolEmpresa.ADMIN);
        assertThat(guardado.isEsSuperAdmin()).isFalse();
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getEmpresa().getId()).isEqualTo(1L);

        ArgumentCaptor<Empresa> empresaCaptor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(empresaCaptor.capture());
        assertThat(empresaCaptor.getValue().getLicenciaEstado()).isEqualTo(LicenciaEstado.PENDIENTE);
    }

    @Test
    void registrarEmpresaConEmailYaRegistradoLanzaExcepcionYNoPersisteNada() {
        Usuario existente = usuario(1L, "existente@test.com", "hash", null, RolEmpresa.ADMIN, false, true);
        when(usuarioRepository.findByEmail("existente@test.com")).thenReturn(Optional.of(existente));

        RegistroEmpresaRequest request = new RegistroEmpresaRequest(
                "Otra Empresa", "Otro", "existente@test.com", "password123");

        assertThatThrownBy(() -> authService.registrarEmpresa(request))
                .isInstanceOf(EmailYaRegistradoException.class);

        verify(empresaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    // --- Helpers ---

    private Empresa empresa(Long id, LicenciaEstado estado) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setNombre("Empresa Test");
        empresa.setLicenciaEstado(estado);
        return empresa;
    }

    private Usuario usuario(Long id, String email, String passwordHash, Empresa empresa,
                             RolEmpresa rol, boolean esSuperAdmin, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario Test");
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordHash);
        usuario.setEmpresa(empresa);
        usuario.setRolEmpresa(rol);
        usuario.setEsSuperAdmin(esSuperAdmin);
        usuario.setActivo(activo);
        return usuario;
    }
}
