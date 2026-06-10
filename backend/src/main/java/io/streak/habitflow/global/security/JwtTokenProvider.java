package io.streak.habitflow.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKeyString;

    private Key secretKey;


    @PostConstruct
    public void init() {
        //TODO BASE64로?
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String email, Long memberId){
        Date now = new Date();
        long tokenValidityInMilliseconds = 1000L * 60 * 60 * 24;
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(email)
                .claim("memberId",memberId)
                .claim("role","ROLE_USER")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
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
}
