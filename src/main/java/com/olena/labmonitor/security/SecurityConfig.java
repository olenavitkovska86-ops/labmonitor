package com.olena.labmonitor.security;

import com.olena.labmonitor.device.security.DeviceAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // For @PreAuthorize
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final DeviceAuthenticationFilter deviceAuthenticationFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, DeviceAuthenticationFilter deviceAuthenticationFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.deviceAuthenticationFilter = deviceAuthenticationFilter;
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
                                response.sendError(HttpStatus.UNAUTHORIZED.value())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
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

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(){
        return new DefaultMethodSecurityExpressionHandler();
    }

    // Validates email+pass during login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource(){
//        CorsConfiguration config = new CorsConfiguration();
//
//        config.setAllowedOrigins(List.of("http://localhost:5173"));
//        config.setAllowedMethods(List.of("GET", "POST", "PUT",
//                "DELETE", "OPTIONS"));
//        config.setAllowedHeaders(List.of("*"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return (CorsConfigurationSource) source;
//    }

}
