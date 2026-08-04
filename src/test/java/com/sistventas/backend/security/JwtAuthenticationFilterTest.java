package com.sistventas.backend.security;

import com.sistventas.backend.entity.RolEmpresa;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// JwtAuthenticationFilter es un OncePerRequestFilter: se testea con
// request/response/chain mockeados (sin levantar Spring) y se verifica qué
// queda seteado en el SecurityContextHolder, que es estado estático global
// y hay que limpiar entre tests para no filtrar entre casos.
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void crearFiltro() {
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenValidoSeteaAuthenticationEnElSecurityContextYSigueLaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(claims.get("esSuperAdmin", Boolean.class)).thenReturn(false);
        when(claims.get("rolEmpresa", String.class)).thenReturn("ADMIN");
        when(claims.get("empresaId", Number.class)).thenReturn(10L);
        when(jwtService.parseClaims("token-valido")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertThat(principal.id()).isEqualTo(5L);
        assertThat(principal.empresaId()).isEqualTo(10L);
        assertThat(principal.esSuperAdmin()).isFalse();
        assertThat(principal.rolEmpresa()).isEqualTo(RolEmpresa.ADMIN);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenValidoDeSuperAdminAgregaRoleSuperAdminYEmpresaIdNulo() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-super-admin");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("esSuperAdmin", Boolean.class)).thenReturn(true);
        when(claims.get("rolEmpresa", String.class)).thenReturn(null);
        when(claims.get("empresaId", Number.class)).thenReturn(null);
        when(jwtService.parseClaims("token-super-admin")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertThat(principal.esSuperAdmin()).isTrue();
        assertThat(principal.empresaId()).isNull();
        assertThat(principal.rolEmpresa()).isNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SUPER_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void sinHeaderAuthorizationNoSeteaAuthenticationYSigueLaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void headerSinPrefijoBearerSeIgnora() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic algo-random");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).parseClaims(any());
    }

    @Test
    void tokenExpiradoLimpiaContextoYSigueLaCadenaSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-expirado");
        when(jwtService.parseClaims("token-expirado")).thenThrow(mock(ExpiredJwtException.class));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenConClaimRolEmpresaInvalidoLimpiaContextoYSigueLaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-rol-invalido");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(claims.get("esSuperAdmin", Boolean.class)).thenReturn(false);
        when(claims.get("rolEmpresa", String.class)).thenReturn("ROL_QUE_NO_EXISTE");
        when(jwtService.parseClaims("token-rol-invalido")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
