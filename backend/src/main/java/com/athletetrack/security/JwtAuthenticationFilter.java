package com.athletetrack.security;

import com.athletetrack.user.UserAccount;
import com.athletetrack.user.UserAccountRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserAccountRepository users;
    public JwtAuthenticationFilter(JwtService jwt, UserAccountRepository users) { this.jwt = jwt; this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                String email = jwt.subject(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
                    if (user != null && jwt.valid(token, user.getUsername())) {
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
