package com.eldercare.security;

import com.eldercare.domain.User;
import com.eldercare.repo.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            User u = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(username));
            return org.springframework.security.core.userdetails.User.builder()
                    .username(u.getUsername())
                    .password(u.getPassword())
                    .disabled(!Boolean.TRUE.equals(u.getEnabled()))
                    .roles(u.getRole())
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration c = new CorsConfiguration();
                    c.setAllowedOriginPatterns(List.of("*"));
                    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    c.setAllowedHeaders(List.of("*"));
                    c.setAllowCredentials(true);
                    return c;
                }))
                .sessionManagement(s -> s.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh.authenticationEntryPoint((req, resp, ex) ->
                        resp.sendError(401, "未认证或登录已过期")));
        return http.build();
    }
}
