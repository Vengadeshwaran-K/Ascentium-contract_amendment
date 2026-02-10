package com.company.contractsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())   // 🔥 REQUIRED for POST
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // ✅ Enable Basic Auth

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password("{noop}admin123") // 🔥 REQUIRED
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
////
////    @Bean
////    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
////
////        http
////                // ✅ Disable CSRF for REST APIs
////                .csrf(csrf -> csrf.disable())
////
////                // ✅ Authorization rules
////                .authorizeHttpRequests(auth -> auth
////                        .requestMatchers(
////                                new AntPathRequestMatcher("/auth/**")
////                        ).permitAll()
////                        .anyRequest().authenticated()
////                )
////
////                // ✅ Disable default login form
////                .httpBasic(Customizer.withDefaults())
////                .formLogin(form -> form.disable());
////
////        return http.build();
////    }
////
////    @Bean
////    public PasswordEncoder passwordEncoder() {
////        return new BCryptPasswordEncoder();
////    }
////
////    @Bean
////    public AuthenticationManager authenticationManager(
////            AuthenticationConfiguration config) throws Exception {
////        return config.getAuthenticationManager();
////    }
}
