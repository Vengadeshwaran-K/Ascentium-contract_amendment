package com.company.contractsystem.config;

import com.company.contractsystem.auth.service.AuthTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

        private final AuthTokenFilter authTokenFilter;

        public SecurityConfig(AuthTokenFilter authTokenFilter) {
                this.authTokenFilter = authTokenFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(
                                                                        jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.getWriter().write(
                                                                        "Unauthorized: " + authException.getMessage());
                                                }))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                new AntPathRequestMatcher("/auth/**"),
                                                                new AntPathRequestMatcher("/login.html"),
                                                                new AntPathRequestMatcher("/login.js"),
                                                                new AntPathRequestMatcher("/dashboard.html"),
                                                                new AntPathRequestMatcher("/script.js"),
                                                                new AntPathRequestMatcher("/style.css"),
                                                                new AntPathRequestMatcher("/"),
                                                                new AntPathRequestMatcher("/error"),
                                                                new AntPathRequestMatcher("/index.html"))
                                                .permitAll()
                                                .requestMatchers(new AntPathRequestMatcher("/admin/users"))
                                                .hasAnyRole("SUPER_ADMIN", "LEGAL_USER")
                                                .requestMatchers(new AntPathRequestMatcher("/admin/**"))
                                                .hasRole("SUPER_ADMIN")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form.disable());

                http.addFilterBefore(authTokenFilter,
                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}
