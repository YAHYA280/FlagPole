package dev.flagpole.api.security;

import dev.flagpole.api.environment.EnvironmentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates SDK requests by environment SDK key, sent as {@code Authorization: Bearer fp_...}
 * or {@code X-SDK-Key: fp_...}. Unknown or missing keys leave the request anonymous; the
 * SDK filter chain then rejects it with 401.
 */
public class SdkKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String SDK_KEY_HEADER = "X-SDK-Key";
    private static final String BEARER_PREFIX = "Bearer ";

    private final EnvironmentRepository environmentRepository;

    public SdkKeyAuthenticationFilter(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String sdkKey = extractKey(request);
        if (sdkKey != null) {
            environmentRepository.findBySdkKey(sdkKey).ifPresent(environment -> {
                SdkPrincipal principal = new SdkPrincipal(
                        environment.getId(),
                        environment.getKey(),
                        environment.getProject().getId(),
                        environment.getProject().getKey());
                SecurityContextHolder.getContext().setAuthentication(new SdkAuthenticationToken(principal));
            });
        }
        chain.doFilter(request, response);
    }

    private static String extractKey(HttpServletRequest request) {
        String explicit = request.getHeader(SDK_KEY_HEADER);
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
