package com.eventsystem.event_management_system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Javni endpointi
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()

                // Finansijski podsistem — samo ZAPOSLENI sa ulogama FINANSIJSKI_KONTROLOR ili MENADZER_DOGADJAJA
                .requestMatchers("/api/budzet/**").hasAnyRole("FINANSIJSKI_KONTROLOR", "MENADZER_DOGADJAJA")
                .requestMatchers("/api/fakture/**").hasAnyRole("FINANSIJSKI_KONTROLOR", "MENADZER_DOGADJAJA")
                .requestMatchers("/api/placanja/**").hasAnyRole("FINANSIJSKI_KONTROLOR", "MENADZER_DOGADJAJA")
                .requestMatchers("/api/troskovi/**").hasAnyRole("FINANSIJSKI_KONTROLOR", "MENADZER_DOGADJAJA")
                .requestMatchers("/api/analiza/**").hasAnyRole("FINANSIJSKI_KONTROLOR", "MENADZER_DOGADJAJA")

                .requestMatchers("/api/lokacija/**").hasAnyRole("MENADZER_DOGADJAJA")
                .requestMatchers("/api/dogadjaj/**").hasAnyRole("KOORDINATOR_PROGRAMA", "MENADZER_DOGADJAJA")
                .requestMatchers("/api/sesija/**").hasAnyRole("KOORDINATOR_PROGRAMA")
                .requestMatchers("/api/govornik/**").hasAnyRole("KOORDINATOR_PROGRAMA")
                .requestMatchers("/api/tip-karte/**").hasAnyRole("KOORDINATOR_PROGRAMA")

                .requestMatchers("/api/sala/**").hasAnyRole("KOORDINATOR_RESURSA")

                // Svi autentifikovani korisnici
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService); // ← prosleđuješ odmah
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
