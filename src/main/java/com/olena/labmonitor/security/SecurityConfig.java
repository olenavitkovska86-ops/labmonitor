package com.olena.labmonitor.security;

import com.olena.labmonitor.common.error.ApiErrorFactory;
import com.olena.labmonitor.device.security.DeviceAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // For @PreAuthorize
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final DeviceAuthenticationFilter deviceAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final ApiErrorFactory errorFactory;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, DeviceAuthenticationFilter deviceAuthenticationFilter,
                          ObjectMapper objectMapper, ApiErrorFactory errorFactory) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.deviceAuthenticationFilter = deviceAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.errorFactory = errorFactory;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/auth/login", "/api/device/**")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, HttpStatus.UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, HttpStatus.FORBIDDEN, "Access denied")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/login").permitAll()
                        .requestMatchers("/auth/logout").authenticated()
                        .requestMatchers("/auth/change-password").authenticated()
                        .requestMatchers("/api/device/**").hasAuthority("DEVICE_INGEST")
                        .requestMatchers(HttpMethod.POST,"/admin/users").hasRole("SUPER_ADMIN")
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/js/**").permitAll()
                        .requestMatchers("/**/*.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(deviceAuthenticationFilter, JwtAuthFilter.class);
        return http.build();
    }

    private void writeSecurityError(jakarta.servlet.http.HttpServletResponse response,
                                    HttpStatus status, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), errorFactory.create(status, message, List.of()));
    }

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(){
        return new DefaultMethodSecurityExpressionHandler();
    }

    // Validates email+password during login.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
