package com.miguel_damasco.DoSafe.security.correlationId;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
            String correlationId = UUID.randomUUID().toString();

            MDC.put("correlationId", correlationId);

            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.clear();
            }
    }
    
}
