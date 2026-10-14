package org.dentalcrm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tope básico por IP para los endpoints públicos (§16): desanima el
 * raspado de cupos y el abuso de tokens. Ventana móvil en memoria,
 * suficiente para una instancia única de la clínica.
 */
@Component
public class PublicRateLimitFilter extends OncePerRequestFilter {

    public static final String RUTA_PUBLICA = "/api/v1/public/";
    public static final int MAXIMO_PETICIONES = 60;
    public static final long VENTANA_MS = 60_000;

    private final Map<String, Deque<Long>> ventanas = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ruta = request.getRequestURI();
        if (!ruta.startsWith(RUTA_PUBLICA)) {
            filterChain.doFilter(request, response);
            return;
        }
        String ip = request.getRemoteAddr() == null ? "desconocida" : request.getRemoteAddr();
        long ahora = System.currentTimeMillis();
        Deque<Long> ventana = ventanas.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (ventana) {
            while (!ventana.isEmpty() && ahora - ventana.peekFirst() > VENTANA_MS) {
                ventana.pollFirst();
            }
            if (ventana.size() >= MAXIMO_PETICIONES) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Demasiadas solicitudes, espera un minuto.\"}");
                return;
            }
            ventana.addLast(ahora);
        }
        if (ventanas.size() > 10_000) {
            ventanas.clear();
        }
        filterChain.doFilter(request, response);
    }
}
