package com.java.StockMarketApi.Utils;

import com.java.StockMarketApi.Models.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JWTManager {
    private static final Logger logger = LoggerFactory.getLogger(JWTManager.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}") // Default 1 hour if not specified
    private int jwtExpiration;

    public record JWTClaims(
            Integer userId,
            String username,
            UserRole role
    ) {}

    private SecretKey getSigningKey() {
        // Ensure the key is at least 256 bits (32 bytes)
        byte[] keyBytes = new byte[32];
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, keyBytes.length));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, UserRole role, String name, Integer id) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpiration);

            return Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setHeaderParam("alg", "HS256")
                    .setSubject(username)
                    .claim("role", role.name())
                    .claim("username", name)
                    .claim("id", id)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Error generating JWT token: ", e);
            throw new RuntimeException("Could not generate token", e);
        }
    }


    public String getRoleFromToken(String token) {
        logger.debug("Processing token for role extraction");

        // Clean the token by removing "Bearer " if present
        String cleanToken = token;
        if (token != null && token.startsWith("Bearer ")) {
            cleanToken = token.substring(7); // Remove "Bearer " prefix
            logger.debug("Removed Bearer prefix from token");
        }

        try {
            logger.debug("Extracting role from cleaned token");
            return getClaimFromToken(cleanToken, claims -> claims.get("role", String.class));
        } catch (ExpiredJwtException e) {
            logger.error("Token has expired");
            throw e;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature");
            throw e;
        } catch (Exception e) {
            logger.error("Error extracting role from token: ", e);
            throw e;
        }
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.error("JWT token has expired");
            return false;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature");
            return false;
        } catch (Exception e) {
            logger.error("Invalid JWT token: ", e);
            return false;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error parsing JWT token: ", e);
            throw e;
        }
    }

    // Additional utility methods
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public Integer getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("id", Integer.class));
    }

    public boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }


    private static final int SALT_ROUNDS = 12;

    /**
     * Encrypt a password using BCrypt
     * @param plainTextPassword The password to encrypt
     * @return The encrypted password
     */
    public static String encryptPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(SALT_ROUNDS));
    }

    /**
     * Verify if a plain text password matches an encrypted password
     * @param plainTextPassword The plain text password to check
     * @param hashedPassword The encrypted password to check against
     * @return true if the passwords match, false otherwise
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}