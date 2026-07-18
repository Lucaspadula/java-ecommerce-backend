package com.sistventas.backend.controller;

import com.sistventas.backend.dto.GoogleLoginRequest;
import com.sistventas.backend.dto.GoogleRegistroEmpresaRequest;
import com.sistventas.backend.dto.GoogleUsuarioNoEncontradoDto;
import com.sistventas.backend.dto.LoginRequest;
import com.sistventas.backend.dto.LoginResponse;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.RegistroEmpresaRequest;
import com.sistventas.backend.exception.UsuarioGoogleNoRegistradoException;
import com.sistventas.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro-empresa")
    public ResponseEntity<MensajeResponse> registrarEmpresa(@Valid @RequestBody RegistroEmpresaRequest request) {
        MensajeResponse response = authService.registrarEmpresa(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google/login")
    public ResponseEntity<?> loginConGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            return ResponseEntity.ok(authService.loginConGoogle(request));
        } catch (UsuarioGoogleNoRegistradoException ex) {
            GoogleUsuarioNoEncontradoDto body = new GoogleUsuarioNoEncontradoDto(
                    "No existe una cuenta con ese email. Registrate primero.",
                    ex.getEmail(),
                    ex.getNombre());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }

    @PostMapping("/google/registrar-empresa")
    public ResponseEntity<MensajeResponse> registrarEmpresaConGoogle(@Valid @RequestBody GoogleRegistroEmpresaRequest request) {
        MensajeResponse response = authService.registrarEmpresaConGoogle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
