package com.szs.v2.configs;

import com.szs.v2.configs.jwt.TokenProvider;
import com.szs.v2.services.UserDetailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.io.IOException;
import java.util.List;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@RequiredArgsConstructor
@Configuration
public class WebSecurityConfig {

    private final UserDetailService userService;

    private final TokenProvider tokenProvider;

    private static final String[] AUTH_WHITELIST = {
            // -- Swagger UI v2
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            // -- Swagger UI v3 (OpenAPI)
            "/v3/api-docs/**",
            "/swagger-ui/**"
            // other public endpoints of your API may be appended to this array
    };

    // 스프링 시큐리티 기능 비활성화
    // 인증/인가 서비스를 모든 곳에 적용하지 않는다 (일반적으로 정적 리소스(이미지, HTML 파일)에 설정한다)
    // 정적 리소스만 스프링 시큐리티 사용을 비활성화하는데 static 하위 경로에 있는 리소스와, h2의 데이터를 확인하는 데 사용하는 h2-console 하위 url을 대상으로 ignoring() 메서드를 사용한다

    @Bean
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring().requestMatchers(toH2Console());
        // return (web) -> web.ignoring().requestMatchers(toH2Console()).requestMatchers("/static/**");
    }

    // 특정 HTTP 요청에 대한 웹 기반 보안 구성
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {

        RequestMatcher printView = (request) -> request.getMethod() == "PUT";

        return http
                .httpBasic(httpBasic -> httpBasic.disable()) // ID, Password 문자열을 Base64로 인코딩하여 전달하는 구조
                .csrf(csrf -> csrf.disable()) // 쿠키 기반이 아닌 JWT 기반이므로 사용하지 않음
                .cors(c -> { // CORS 설정
                            CorsConfigurationSource source = request -> {
                                // Cors 허용 패턴
                                CorsConfiguration config = new CorsConfiguration();
                                config.setAllowedOrigins(
                                        List.of("*")
                                );
                                config.setAllowedMethods(
                                        List.of("*")
                                );
                                return config;
                            };
                            c.configurationSource(source);
                        }
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Spring Security 세션 정책 : 세션을 생성 및 사용하지 않음
                .authorizeHttpRequests(authorize  -> authorize
                        .requestMatchers(antMatcher("/user/api/signin")).permitAll()
                        .requestMatchers(antMatcher("/user/api/signup")).permitAll()
                        .requestMatchers(antMatcher("/blog/api/articles")).permitAll()
                        .requestMatchers(antMatcher("/blog/api/articles/**")).authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(new TokenAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class) // JWT 인증 필터 적용
                .exceptionHandling(exception -> { // 에러 핸들링

                    exception.accessDeniedHandler(new AccessDeniedHandler() {
                        @Override
                        public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                            // 권한 문제가 발생했을 때 이 부분을 호출한다.
                            response.setStatus(403);
                            response.setCharacterEncoding("utf-8");
                            response.setContentType("text/html; charset=UTF-8");
                            response.getWriter().write("권한이 없는 사용자입니다.");
                        }
                    });

                    exception.authenticationEntryPoint(new AuthenticationEntryPoint() {
                        @Override
                        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                            // 인증문제가 발생했을 때 이 부분을 호출한다.
                            response.setStatus(401);
                            response.setCharacterEncoding("utf-8");
                            response.setContentType("text/html; charset=UTF-8");
                            response.getWriter().write("인증되지 않은 사용자입니다.");
                        }
                    });
                })

                .build();
    }

    // 인증 관리자 관련 설정
    // https://covenant.tistory.com/277
    // 스프링 시큐리티의 인증을 담당하는 AuthenticationManager는 이전 설정 방법으로 authenticationManagerBuilder를 이용해서 userDetailsService와 passwordEncoder를 설정해주어야 했습니다.
    // 그러나 변경된 설정에서는 AuthenticationManager 빈 생성 시 스프링의 내부 동작으로 인해 위에서 작성한 UserSecurityService와 PasswordEncoder가 자동으로 설정됩니다.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /*
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, BCryptPasswordEncoder bCryptPasswordEncoder, UserDetailService userDetailService) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userService)
                .passwordEncoder(bCryptPasswordEncoder)
                .and()
                .build();
    }
    */

    // 패스워드 인코더로 사용할 빈 등록
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
        Ref
     */
    // https://velog.io/@woosim34/Spring-Security-6.1.0%EC%97%90%EC%84%9C-is-deprecated-and-marked-for-removal-%EC%98%A4%EB%A5%98
    // https://forwe.tistory.com/66  -> HandlerMappingIntrospector introspector

}