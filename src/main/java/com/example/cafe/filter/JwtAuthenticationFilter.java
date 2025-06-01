package com.example.cafe.filter;

import com.example.cafe.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.GrantedAuthority;
import jakarta.servlet.http.Cookie;

import java.util.List;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        if (jwt != null) {
            try {
                String username = jwtUtil.getUsernameFromToken(jwt);

                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    boolean isValid = jwtUtil.validateToken(jwt, userDetails);

                    System.out.println("Username: " + username);
                    System.out.println("UserDetails username: " + userDetails.getUsername());
                    System.out.println("Token valid: " + isValid);

                    if (isValid && SecurityContextHolder.getContext().getAuthentication() == null) {
                        List<GrantedAuthority> authorities = jwtUtil.getAuthoritiesFromToken(jwt);

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        System.out.println("Authenticated user: " + username + " with roles: " + authorities);
                    } else {
                        System.out.println("User already authenticated or token not valid");
                    }
                }
            } catch (Exception e) {
                System.out.println("JWT error: " + e.getMessage());
            }
        } else {
            System.out.println("No JWT found in request");
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
