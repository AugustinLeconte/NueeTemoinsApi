package com.nueetemoins.backend.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final NeonJwtVerifier verifier;
    public JwtFilter(NeonJwtVerifier verifier) { this.verifier = verifier; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            logger.warn("Pas de header Authorization sur " + req.getRequestURI());
            res.sendError(401); return;
        }
        try {
            String userId;
            try {
                userId = verifier.verify(header.substring(7));
            } catch (Exception e) {
                logger.warn("JWT refusé : " + e.getClass().getSimpleName() + " - " + e.getMessage());
                res.sendError(401);
                return;
            }
            req.setAttribute("userId", userId);
            chain.doFilter(req, res);
        } catch (Exception e) {
            logger.warn("JWT refusé : " + e.getClass().getSimpleName() + " - " + e.getMessage());
            res.sendError(401);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        boolean isGet = "GET".equalsIgnoreCase(method);
        return isGet && (path.equals("/health")
                || path.startsWith("/api/images/")
                || (path.startsWith("/api/cards") && !path.endsWith("/admin")));
    }
}