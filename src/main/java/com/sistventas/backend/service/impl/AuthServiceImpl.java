package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.GoogleLoginRequest;
import com.sistventas.backend.dto.GoogleRegistroEmpresaRequest;
import com.sistventas.backend.dto.LoginRequest;
import com.sistventas.backend.dto.LoginResponse;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.RegistroEmpresaRequest;
import com.sistventas.backend.dto.UsuarioLoginDto;
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
import com.sistventas.backend.security.JwtService;
import com.sistventas.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestClient restClient;
    private final String googleClientId;

    public AuthServiceImpl(
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RestClient.Builder restClientBuilder,
            @Value("${sistventas.google.client-id}") String googleClientId) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Inyectado en vez de RestClient.create(): Spring Boot autoconfigura
        // un RestClient.Builder equivalente (mismo comportamiento en
        // producción), pero al recibirlo por constructor los tests pueden
        // pasar uno atado a un MockRestServiceServer para simular las
        // respuestas de Google sin pegarle a la red real.
        this.restClient = restClientBuilder.build();
        this.googleClientId = googleClientId;
    }

    @Override
    @Transactional
    public MensajeResponse registrarEmpresa(RegistroEmpresaRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailYaRegistradoException();
        }

        Empresa empresa = new Empresa();
        empresa.setNombre(request.nombreEmpresa());
        empresa.setLicenciaEstado(LicenciaEstado.PENDIENTE);
        empresa.setFechaAlta(LocalDateTime.now());
        empresa = empresaRepository.save(empresa);

        Usuario usuario = new Usuario();
        usuario.setEmpresa(empresa);
        usuario.setNombre(request.nombreUsuario());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRolEmpresa(RolEmpresa.ADMIN);
        usuario.setEsSuperAdmin(false);
        usuario.setActivo(true);
        usuario.setFechaAlta(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return new MensajeResponse("Cuenta creada. Tu acceso queda pendiente de aprobación.");
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        validarAccesoUsuario(usuario);

        return construirLoginResponse(usuario);
    }

    @Override
    @Transactional
    public LoginResponse loginConGoogle(GoogleLoginRequest request) {
        GoogleTokenInfo tokenInfo = verificarTokenGoogle(request.idToken());

        Usuario usuario = usuarioRepository.findByEmail(tokenInfo.email())
                .orElseThrow(() -> new UsuarioGoogleNoRegistradoException(tokenInfo.email(), tokenInfo.nombre()));

        validarAccesoUsuario(usuario);

        if (usuario.getGoogleSub() == null) {
            usuario.setGoogleSub(tokenInfo.sub());
            usuarioRepository.save(usuario);
        }

        return construirLoginResponse(usuario);
    }

    @Override
    @Transactional
    public MensajeResponse registrarEmpresaConGoogle(GoogleRegistroEmpresaRequest request) {
        GoogleTokenInfo tokenInfo = verificarTokenGoogle(request.idToken());

        if (usuarioRepository.findByEmail(tokenInfo.email()).isPresent()) {
            throw new EmailYaRegistradoException();
        }

        Empresa empresa = new Empresa();
        empresa.setNombre(request.nombreEmpresa());
        empresa.setLicenciaEstado(LicenciaEstado.PENDIENTE);
        empresa.setFechaAlta(LocalDateTime.now());
        empresa = empresaRepository.save(empresa);

        Usuario usuario = new Usuario();
        usuario.setEmpresa(empresa);
        usuario.setNombre(tokenInfo.nombre());
        usuario.setEmail(tokenInfo.email());
        usuario.setPasswordHash(null);
        usuario.setGoogleSub(tokenInfo.sub());
        usuario.setRolEmpresa(RolEmpresa.ADMIN);
        usuario.setEsSuperAdmin(false);
        usuario.setActivo(true);
        usuario.setFechaAlta(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return new MensajeResponse("Cuenta creada. Tu acceso queda pendiente de aprobación.");
    }

    /**
     * Chequeo de acceso compartido entre login() y loginConGoogle(): cuenta
     * activa y, si no es Super Admin, licencia de la empresa en estado ACTIVA.
     */
    private void validarAccesoUsuario(Usuario usuario) {
        if (!usuario.isActivo()) {
            throw new CuentaDeshabilitadaException();
        }

        if (!usuario.isEsSuperAdmin()) {
            Empresa empresa = usuario.getEmpresa();
            switch (empresa.getLicenciaEstado()) {
                case PENDIENTE -> throw new LicenciaNoActivaException("Tu empresa está pendiente de aprobación.");
                case SUSPENDIDA -> throw new LicenciaNoActivaException("La licencia de tu empresa está suspendida.");
                case VENCIDA -> throw new LicenciaNoActivaException("La licencia de tu empresa venció.");
                case ACTIVA -> { }
            }
        }
    }

    private LoginResponse construirLoginResponse(Usuario usuario) {
        String token = jwtService.generateToken(usuario);
        Empresa empresa = usuario.getEmpresa();
        UsuarioLoginDto usuarioDto = new UsuarioLoginDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.isEsSuperAdmin(),
                usuario.getRolEmpresa(),
                empresa != null ? empresa.getId() : null,
                empresa != null ? empresa.getNombre() : null,
                empresa != null ? empresa.getLogoUrl() : null
        );

        return new LoginResponse(token, usuarioDto);
    }

    /**
     * Verifica un ID token de Google Identity Services contra el endpoint
     * oficial de Google (sin agregar la dependencia google-api-client).
     * Google valida firma y expiración del lado suyo; acá solo confirmamos
     * que la respuesta sea 200, que el token haya sido emitido para esta
     * aplicación (aud) y que el email esté verificado.
     */
    private GoogleTokenInfo verificarTokenGoogle(String idToken) {
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

    private record GoogleTokenInfo(String email, String nombre, String sub) { }
}
