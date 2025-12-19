package com.srmanagement.config;

import com.srmanagement.security.CustomUserDetailsService;
import com.srmanagement.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security 설정 클래스
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 비밀번호 인코더 Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증 프로바이더 Bean
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 인증 매니저 Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 보안 필터 체인 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화: JWT 토큰 기반 인증을 사용하므로 CSRF 보호가 필요하지 않음
                // JWT 토큰 자체가 요청의 인증 수단이며, 쿠키를 사용하지 않으므로
                // CSRF 공격에 취약하지 않음 (Stateless REST API)
                .csrf(csrf -> csrf.disable())
                
                // 세션 관리 설정 (Stateless)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 예외 처리 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 API는 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/surveys/template").permitAll()  // CSV 템플릿 다운로드 허용
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Wiki 파일 다운로드 허용 (이미지 표시를 위해)
                        .requestMatchers(HttpMethod.GET, "/api/wiki/files/**").permitAll()
                        // 정적 리소스 허용 (프론트엔드 배포용)
                        .requestMatchers(
                            "/",
                            "/index.html",
                            "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/static/**", "/assets/**").permitAll()
                        .requestMatchers("/*.js", "/*.mjs", "/*.css", "/*.png", "/*.svg", "/*.ico").permitAll()
                        // ADMIN 전용 API
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        // .requestMatchers(HttpMethod.DELETE, "/api/sr/**").hasRole("ADMIN") // 서비스 계층에서 권한 체크
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                
                // H2 콘솔을 위한 프레임 옵션 설정
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                
                // 인증 프로바이더 설정
                .authenticationProvider(authenticationProvider())
                
                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
