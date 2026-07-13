package io.streak.habitflow.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.streak.habitflow.global.error.SecurityErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityErrorWriter securityErrorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        // 토큰 없음 → 그대로 진행 (protected API면 entryPoint가 "로그인이 필요합니다" 반환)
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        JwtTokenProvider.JwtValidationResult result = jwtTokenProvider.validateTokenResult(token);
        // 토큰은 있지만 invalid/expired → 401 즉시 응답
        if (result != JwtTokenProvider.JwtValidationResult.VALID) {
            String message = switch (result) {
                case EXPIRED     -> "만료된 토큰입니다. 다시 로그인해주세요.";
                case MALFORMED   -> "형식이 올바르지 않은 토큰입니다.";
                case UNSUPPORTED -> "지원하지 않는 토큰입니다.";
                default          -> "유효하지 않은 토큰입니다.";
            };
            securityErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, message);
            return;
        }
        // 블랙리스트(로그아웃된 토큰) 확인
        String isLogout = (String) redisTemplate.opsForValue().get("BLACKLIST:" + token);
        if (!ObjectUtils.isEmpty(isLogout)) {
            log.warn("로그아웃된 토큰으로 접근 시도 발생");
            securityErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    "로그아웃된 토큰입니다. 다시 로그인해주세요.");
            return;
        }

        Claims claims = jwtTokenProvider.getClaims(token); // 또는 parseClaims 헬퍼
        if (!"ACCESS".equals(claims.get("tokenType", String.class))) {
            securityErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
            return;
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if( bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
