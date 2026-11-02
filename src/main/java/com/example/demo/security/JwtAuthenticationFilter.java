package com.example.demo.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if(token == null){
            filterChain.doFilter(request,response);
            return;
        }
        if(!token.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }
        String tokenValue = token.substring(7);
        try{
            long userId = jwtService.extractUserId(tokenValue);
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        }catch (JwtException | IllegalArgumentException e){
            logger.debug("Jwt is not valid");
        }
        filterChain.doFilter(request,response);

    }

}
