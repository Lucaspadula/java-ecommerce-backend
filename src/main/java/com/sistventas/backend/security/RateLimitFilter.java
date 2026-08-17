package com.sistventas.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Rate-limiting en memoria por IP para las rutas más expuestas a abuso
 * automatizado: login (fuerza bruta) y la tienda pública (sin JWT, sin
 * captcha — cualquiera puede pegarle sin fricción). Es en memoria porque
 * el backend corre como una sola instancia; si en algún momento se escala
 * horizontalmente, esto tiene que pasar a un store compartido (Redis).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern PEDIDO_CREATE = Pattern.compile("^/api/public/tienda/[^/]+/pedidos$");
    private static final Pattern PEDIDO_CONSULTA = Pattern.compile("^/api/public/tienda/[^/]+/pedidos/[^/]+$");
    private static final Pattern GRABADO_FOTO = Pattern.compile("^/api/public/tienda/[^/]+/grabado/foto$");

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Regla regla = resolverRegla(request);
        if (regla != null) {
            String key = regla.name() + ":" + request.getRemoteAddr();
            Bucket bucket = buckets.computeIfAbsent(key, k -> regla.nuevoBucket());
            if (!bucket.tryConsume(1)) {
                response.setStatus(429); // Too Many Requests — no está en HttpServletResponse.SC_*
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Demasiados intentos. Probá de nuevo en unos minutos.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Regla resolverRegla(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            return Regla.LOGIN;
        }
        if ("POST".equals(method) && PEDIDO_CREATE.matcher(path).matches()) {
            return Regla.PEDIDO_CREATE;
        }
        if ("GET".equals(method) && PEDIDO_CONSULTA.matcher(path).matches()) {
            return Regla.PEDIDO_CONSULTA;
        }
        if ("POST".equals(method) && GRABADO_FOTO.matcher(path).matches()) {
            return Regla.GRABADO_FOTO;
        }
        return null;
    }

    /** Reglas: cuántas requests se permiten por IP en la ventana de tiempo. */
    private enum Regla {
        LOGIN(8, Duration.ofMinutes(5)),
        PEDIDO_CREATE(10, Duration.ofMinutes(5)),
        PEDIDO_CONSULTA(20, Duration.ofMinutes(5)),
        // Subida de archivo, sin JWT: más cara que un simple POST de datos,
        // límite más ajustado que PEDIDO_CREATE.
        GRABADO_FOTO(6, Duration.ofMinutes(5));

        private final int capacidad;
        private final Duration ventana;

        Regla(int capacidad, Duration ventana) {
            this.capacidad = capacidad;
            this.ventana = ventana;
        }

        Bucket nuevoBucket() {
            Bandwidth limite = Bandwidth.classic(capacidad, Refill.greedy(capacidad, ventana));
            return Bucket.builder().addLimit(limite).build();
        }
    }
}
