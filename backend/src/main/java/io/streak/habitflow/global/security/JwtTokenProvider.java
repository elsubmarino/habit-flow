package io.streak.habitflow.global.security;

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
        //TODO BASE64로?
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String email, Long memberId){
        long tokenValidityInMilliseconds = 1000L * 60 * 30;
        return buildToken(email, memberId, tokenValidityInMilliseconds);
    }

    public String createRefreshToken(String email, Long memberId){
        long tokenValidityInMilliseconds = 1000L * 60 * 60 * 24 * 14;
        return buildToken(email, memberId, tokenValidityInMilliseconds);
    }

    public String buildToken(String email, Long memberId, long validityTime){
        Instant now = Instant.now(clock);
        Instant validity = now.plusMillis(validityTime);

        return Jwts.builder()
                .setSubject(email)
                .claim("memberId",memberId)
                .claim("role","ROLE_USER")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(validity))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getRemainingExpiration(String token){
        Date expiration = Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token).getBody().getExpiration();
        long now = new Date().getTime();
        return Math.max(0, expiration.getTime() - now);
    }

    public Authentication getAuthentication(String token){
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String email = claims.getSubject();

        Long memberId = claims.get("memberId", Long.class);
        String role = claims.get("role", String.class);

        UserPrincipal userPrincipal = new UserPrincipal(memberId, email, role);

        return new UsernamePasswordAuthenticationToken(userPrincipal, "", userPrincipal.getAuthorities());
    }

    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        }catch(SecurityException |MalformedJwtException e){
            log.info("잘못된 JWT 서명입니다.");
        }catch(ExpiredJwtException e){
            log.info("만료된 JWT 토큰입니다.");
        }catch(UnsupportedJwtException e){
            log.info("지원되지 않는 JWT 토큰입니다.");
        }catch(IllegalArgumentException e){
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    public String getEmail(String refreshToken){
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody()
                .getSubject();
    }
}
