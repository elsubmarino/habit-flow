package io.streak.habitflow.global.security.oauth;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOauth2AuthorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        httpCookieOauth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member == null) {
            getRedirectStrategy().sendRedirect(request, response,
                    "http://localhost:3000/oauth2/redirect?error=" +
                            URLEncoder.encode("존재하지 않는 회원입니다.", StandardCharsets.UTF_8));
            return;
        }

        String accessToken = jwtTokenProvider.createAccessToken(email,member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(email,member.getId());

        redisTemplate.opsForValue().set(
                "REFRESH_TOKEN:"+email,
                refreshToken,
                14,
                TimeUnit.DAYS
        );
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(60 * 60 * 24 *14)//14일
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String targetUrl = UriComponentsBuilder
                .fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token",accessToken)
                .build()
                .toString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
