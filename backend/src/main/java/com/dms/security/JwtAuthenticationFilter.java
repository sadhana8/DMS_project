package com.dms.security;

import com.dms.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that authenticates every HTTP request by validating the JWT
 * present in the {@code Authorization} header.
 *
 * <p>
 * Extends {@link OncePerRequestFilter} to guarantee the filter logic runs
 * exactly once per request/response cycle, even in forward or include
 * scenarios.
 *
 * <h2>Processing flow</h2>
 * <ol>
 * <li>Read the {@code Authorization} header; skip the filter entirely if it is
 * absent or does not start with {@code "Bearer "}.</li>
 * <li>Extract the JWT (everything after "Bearer ").</li>
 * <li>Parse the username (e-mail) from the token via {@link JwtUtil}.</li>
 * <li>If no authentication is already set in the {@link SecurityContextHolder},
 * load the {@link UserDetails} from the database and validate the token
 * signature and expiry.</li>
 * <li>On success, create a {@link UsernamePasswordAuthenticationToken} and
 * store it in the {@link SecurityContextHolder} so downstream filters and
 * controllers see the request as authenticated.</li>
 * <li>Any JWT parsing exception is caught and logged; the request continues
 * unauthenticated (Spring Security will reject it at the access-control
 * layer).</li>
 * </ol>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see JwtUtil
 * @see CustomUserDetailsService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Utility for parsing, validating, and generating JWT tokens.
     */
    private final JwtUtil jwtUtil;

    /**
     * Loads user details (roles, enabled flag) from the database by e-mail.
     */
    private final CustomUserDetailsService userDetailsService;

    /**
     * Core filter method executed once per request.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response (not modified by this filter)
     * @param filterChain the remaining filter chain; must always be invoked
     * @throws ServletException if the filter chain throws a servlet error
     * @throws IOException if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Skip filter if Authorization header is missing or not a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtUtil.extractUsername(jwt);

            // Only authenticate if not already authenticated in the current request
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken
                            = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
