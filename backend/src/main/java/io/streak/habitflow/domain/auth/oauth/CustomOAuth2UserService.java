package io.streak.habitflow.domain.auth.oauth;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.member.type.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String)attributes.get("email");
        String name = (String)attributes.get("name");

        Member member = memberRepository.findByEmail(email)
                .orElseGet(()-> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .name(name)
                                .memberRole(MemberRole.USER)
                                .build()
                ));
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getMemberRole().getKey())),
                attributes,
                "email"
        );
    }
}
