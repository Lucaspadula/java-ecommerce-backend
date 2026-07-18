package com.sistventas.backend.security;

import com.sistventas.backend.entity.RolEmpresa;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee el header Authorization: Bearer <token>, valida el JWT y setea el
 * SecurityContext. Stateless — no crea ni depende de sesiones HTTP.
 * Cualquier token ausente, inválido o expirado simplemente deja la request
 * sin autenticar (authorizeHttpRequests decide si igual puede pasar, ej.
 * /api/auth/** y /actuator/**).
 *
 * El principal seteado en el SecurityContext es un UserPrincipal (no un Long
 * ni un username) armado acá mismo a partir de los claims del token. Así
 * cualquier controller obtiene id/empresaId/rol vía @AuthenticationPrincipal
 * sin volver a tocar el JWT — este es el único lugar que lo parsea.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseClaims(token);
                Long usuarioId = Long.valueOf(claims.getSubject());
                boolean esSuperAdmin = Boolean.TRUE.equals(claims.get("esSuperAdmin", Boolean.class));
                String rolEmpresaClaim = claims.get("rolEmpresa", String.class);
                Number empresaIdClaim = claims.get("empresaId", Number.class);

                Long empresaId = empresaIdClaim != null ? empresaIdClaim.longValue() : null;
                RolEmpresa rolEmpresa = rolEmpresaClaim != null ? RolEmpresa.valueOf(rolEmpresaClaim) : null;

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (esSuperAdmin) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                }
                if (rolEmpresaClaim != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + rolEmpresaClaim));
                }

                UserPrincipal principal = new UserPrincipal(usuarioId, empresaId, esSuperAdmin, rolEmpresa);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
