package com.sistventas.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// RateLimitFilter guarda los buckets en un ConcurrentHashMap por instancia
// (clave: regla + IP), así que cada test arranca con un filtro nuevo para no
// arrastrar contadores de un caso a otro.
//
// Nota sobre "ventanas de tiempo distintas resetean el contador": el refill
// del Bucket4j (Refill.greedy) usa el reloj real (nanoTime) por dentro de
// Regla.nuevoBucket() — el filtro no recibe un Clock inyectable, así que no
// hay forma de mockear el paso del tiempo sin tocar el código de producción.
// La ventana real es de 5 minutos, demasiado larga para un test unitario.
// Se deja sin cubrir esa rama específica; lo que sí se cubre acá es que IPs
// o rutas distintas usan buckets independientes (mismo mecanismo que hace
// que una ventana nueva por IP arranque sin heredar el conteo de otra).
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void rutaSinReglaSiempreDejaPasarSinTocarElBucket() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        when(request.getRequestURI()).thenReturn("/api/productos");
        when(request.getMethod()).thenReturn("GET");

        for (int i = 0; i < 20; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void dentroDelLimiteDeLoginLlamaLaCadenaLasVecesPermitidas() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");

        for (int i = 0; i < 8; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(8)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void superarElLimiteDeLoginResponde429YNoLlamaLaCadena() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("5.6.7.8");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        for (int i = 0; i < 8; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(8)).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        assertThat(body.toString()).contains("Demasiados intentos");
    }

    @Test
    void ipsDistintasTienenBucketsIndependientes() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("1.1.1.1", "2.2.2.2");

        for (int i = 0; i < 8; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        // Nueva IP (todavía dentro de su propio límite): no debería estar bloqueada.
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(9)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void pedidoCreateYPedidoConsultaUsanBucketsIndependientesDeLogin() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");

        when(request.getRequestURI()).thenReturn("/api/public/tienda/mi-tienda/pedidos");
        when(request.getMethod()).thenReturn("POST");
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        when(request.getRequestURI()).thenReturn("/api/public/tienda/mi-tienda/pedidos/abc123");
        when(request.getMethod()).thenReturn("GET");
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(11)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
}
