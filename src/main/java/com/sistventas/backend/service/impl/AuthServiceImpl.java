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
import com.sistventas.backend.exception.UsuarioGoogleNoRegistradoException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.UsuarioRepository;
import com.sistventas.backend.security.GoogleTokenVerifier;
import com.sistventas.backend.security.JwtService;
import com.sistventas.backend.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthServiceImpl(
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
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
        GoogleTokenVerifier.GoogleTokenInfo tokenInfo = googleTokenVerifier.verificar(request.idToken());

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
        GoogleTokenVerifier.GoogleTokenInfo tokenInfo = googleTokenVerifier.verificar(request.idToken());

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
}
