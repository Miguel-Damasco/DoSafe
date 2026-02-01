package com.miguel_damasco.DoSafe.security.jwt;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.miguel_damasco.DoSafe.security.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    private final ApplicationContext context;

    public JwtAuthenticationFilter(JwtUtil pJwtUtil, ApplicationContext pContext) {
        this.jwtUtil = pJwtUtil;
        this.context = pContext;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/login") || path.equals("/register");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
      
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")) {

            token = authHeader.substring(7);
            username = this.jwtUtil.extractUsername(token);
        }

        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

           UserDetails userDetails = context.getBean(CustomUserDetailsService.class).loadUserByUsername(username);


           if(jwtUtil.validateToken(token, userDetails)) {

                var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
           }
        }

        filterChain.doFilter(request, response);
    }
}
