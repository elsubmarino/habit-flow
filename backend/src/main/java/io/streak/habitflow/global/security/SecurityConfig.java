package io.streak.habitflow.global.security;

import io.streak.habitflow.global.error.SecurityErrorWriter;
import io.streak.habitflow.global.security.jwt.JwtAuthenticationFilter;
import io.streak.habitflow.global.security.jwt.JwtTokenProvider;
import io.streak.habitflow.global.security.oauth.CustomOAuth2UserService;
import io.streak.habitflow.global.security.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import io.streak.habitflow.global.security.oauth.OAuth2LoginSuccessHandler;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOauth2AuthorizationRequestRepository;
    private final SecurityErrorWriter securityErrorWriter;

    @Value("${app.cors-allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors->cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth->auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/api/auth/**","/oauth2/**","/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/members").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate, securityErrorWriter),
                        UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(httpCookieOauth2AuthorizationRequestRepository))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            String message = exception.getMessage() != null
                                    ? exception.getMessage()
                                    : "소셜 로그인에 실패했습니다.";
                            securityErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, message);
                        })
                )
                .exceptionHandling(ex->ex
                        .authenticationEntryPoint((request,response,authException)->{
                            String message = authException.getMessage() != null
                                    ? authException.getMessage()
                                    : "로그인이 필요합니다.";
                            securityErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, message);
                        })
                        .accessDeniedHandler((request,response,accessDeniedException)->{
                            String message = accessDeniedException.getMessage() != null
                                    ? accessDeniedException.getMessage()
                                    : "접근 권한이 없습니다.";
                            securityErrorWriter.write(request, response, HttpStatus.FORBIDDEN, message);
                        })
                );
        ;
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",configuration);
        return source;
    }
}
