package io.streak.habitflow.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final Clock clock;

    @Value("${jwt.secret}")
    private String secretKeyString;

    private Key secretKey;


    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String email, Long memberId){
        long tokenValidityInMilliseconds = 1000L * 60 * 30;
        return buildToken(email, memberId, tokenValidityInMilliseconds, "ACCESS");
    }

    public String createRefreshToken(String email, Long memberId){
        long tokenValidityInMilliseconds = 1000L * 60 * 60 * 24 * 14;
        return buildToken(email, memberId, tokenValidityInMilliseconds, "REFRESH");
    }

    public String buildToken(String email, Long memberId, long validityTime, String tokenType){
        Instant now = Instant.now(clock);
        Instant validity = now.plusMillis(validityTime);

        return Jwts.builder()
                .subject(email)
                .claim("publicMemberId",memberId)
                .claim("role","ROLE_USER")
                .claim("tokenType",tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(validity))
                .signWith(secretKey)
                .compact();
    }

    public Long getRemainingExpiration(String token){
        Date expiration = jwtParser()
                .parseClaimsJws(token).getBody().getExpiration();
        long now = new Date().getTime();
        return Math.max(0, expiration.getTime() - now);
    }

    public Authentication getAuthentication(String token){
        Claims claims = jwtParser().parseClaimsJws(token).getBody();

        String email = claims.getSubject();

        Long memberId = claims.get("publicMemberId", Long.class);
        String role = claims.get("role", String.class);

        UserPrincipal userPrincipal = new UserPrincipal(memberId, email, role);

        return new UsernamePasswordAuthenticationToken(userPrincipal, "", userPrincipal.getAuthorities());
    }

    public boolean validateToken(String token){
        return validateTokenResult(token) == JwtValidationResult.VALID;
    }

    public JwtValidationResult validateTokenResult(String token) {
        try {
            jwtParser().parseClaimsJws(token);
            return JwtValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            return JwtValidationResult.EXPIRED;
        } catch (MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
            return JwtValidationResult.MALFORMED;
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
            return JwtValidationResult.UNSUPPORTED;
        } catch (SecurityException | IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
            return JwtValidationResult.INVALID;
        }
    }

    public String getEmail(String refreshToken){
        return jwtParser()
                .parseSignedClaims(refreshToken)
                .getPayload()
                .getSubject();
    }

    public enum JwtValidationResult {
        VALID, EXPIRED, INVALID, UNSUPPORTED, MALFORMED
    }

    public Claims getClaims(String token){
        return jwtParser()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtParser jwtParser(){
        return Jwts.parser()
                .verifyWith((SecretKey)secretKey)
                .clockSkewSeconds(30)
                .build();
    }
}
