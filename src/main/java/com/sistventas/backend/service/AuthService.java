package com.sistventas.backend.service;

import com.sistventas.backend.dto.GoogleLoginRequest;
import com.sistventas.backend.dto.GoogleRegistroEmpresaRequest;
import com.sistventas.backend.dto.LoginRequest;
import com.sistventas.backend.dto.LoginResponse;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.RegistroEmpresaRequest;

public interface AuthService {

    MensajeResponse registrarEmpresa(RegistroEmpresaRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse loginConGoogle(GoogleLoginRequest request);

    MensajeResponse registrarEmpresaConGoogle(GoogleRegistroEmpresaRequest request);
}
