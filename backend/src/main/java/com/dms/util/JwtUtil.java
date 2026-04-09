package com.dms.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for all JWT (JSON Web Token) operations.
 *
 * <p>
 * Handles generation, parsing, and validation of both <em>access tokens</em>
 * and <em>refresh tokens</em> using the HMAC-SHA-256 algorithm.
 *
 * <h2>Token types</h2>
 * <ul>
 * <li><b>Access token</b> – short-lived (default 24 h via
 * {@code app.jwt.expiration}). Sent with every API request in the
 * {@code Authorization: Bearer} header.</li>
 * <li><b>Refresh token</b> – long-lived (default 7 days via
 * {@code app.jwt.refresh-expiration}). Used once to obtain a new access token
 * without re-entering credentials. Stored in the database so it can be
 * revoked.</li>
 * </ul>
 *
 * <h2>Secret key</h2>
 * The signing key is read from {@code app.jwt.secret} as a Base64-encoded
 * string. Generate a secure value with:
 * <pre>{@code
 * openssl rand -hex 32
 * }</pre>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@Slf4j
public class JwtUtil {

    /**
     * Base64-encoded HMAC-SHA-256 secret key. Injected from
     * {@code app.jwt.secret} in {@code application.properties}.
     */
    @Value("${app.jwt.secret}")
    private String secret;

    /**
     * Access-token lifetime in milliseconds. Default: 86 400 000 ms = 24 hours.
     */
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Refresh-token lifetime in milliseconds. Default: 604 800 000 ms = 7 days.
     */
    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    // ── Extraction helpers ────────────────────────────────────────────────
    /**
     * Extracts the subject (e-mail address) from a JWT.
     *
     * @param token a signed JWT string
     * @return the {@code sub} claim value (the user's e-mail)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts an arbitrary claim from a JWT using the supplied resolver
     * function.
     *
     * @param <T> the expected return type of the claim
     * @param token a signed JWT string
     * @param claimsResolver a function that maps the full {@link Claims} object
     * to the desired claim value
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ── Generation ────────────────────────────────────────────────────────
    /**
     * Generates an access token for the given user with no extra claims.
     *
     * @param userDetails the authenticated user's {@link UserDetails}
     * @return a signed JWT access-token string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates an access token with additional custom claims merged into the
     * payload.
     *
     * @param extraClaims key-value pairs to include in the token payload (e.g.
     * {@code {"role": "ADMIN"}})
     * @param userDetails the authenticated user's {@link UserDetails}
     * @return a signed JWT access-token string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Generates a long-lived refresh token for the given user.
     *
     * <p>
     * Refresh tokens carry only the {@code sub} claim (no roles or extra data)
     * to minimise the blast radius if one is ever intercepted.
     *
     * @param userDetails the authenticated user's {@link UserDetails}
     * @return a signed JWT refresh-token string
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    // ── Validation ────────────────────────────────────────────────────────
    /**
     * Validates a JWT against the expected user and checks its expiry.
     *
     * @param token the JWT to validate
     * @param userDetails the {@link UserDetails} whose e-mail must match the
     * token subject
     * @return {@code true} if the token is structurally valid, signed with the
     * correct key, not yet expired, and belongs to the supplied user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // ── Private helpers ───────────────────────────────────────────────────
    /**
     * Internal factory that builds and signs a JWT with the given parameters.
     *
     * @param extraClaims additional payload entries
     * @param userDetails source of the subject ({@code sub}) claim
     * @param expiration lifetime in milliseconds from now
     * @return a compact, URL-safe signed JWT string
     */
    private String buildToken(Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Returns {@code true} if the token's expiry date is in the past.
     *
     * @param token a signed JWT string
     * @return {@code true} when expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT.
     *
     * @param token a signed JWT string
     * @return the {@code exp} claim as a {@link Date}
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parses and verifies the signature of a JWT, returning all claims.
     *
     * @param token a signed JWT string
     * @return the verified {@link Claims} payload
     * @throws JwtException if the token is malformed, expired, or the signature
     * is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Decodes the Base64 secret and constructs an HMAC-SHA key.
     *
     * @return the {@link Key} used to sign and verify JWTs
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
