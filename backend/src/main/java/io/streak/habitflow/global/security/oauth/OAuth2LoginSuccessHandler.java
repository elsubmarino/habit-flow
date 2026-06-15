package io.streak.habitflow.global.security.oauth;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.global.security.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        String token = jwtTokenProvider.createAccessToken(email,member.getId());
        String targetUrl = UriComponentsBuilder
                .fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token",token)
                .build()
                .toString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
