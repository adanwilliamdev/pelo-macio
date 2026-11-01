package com.pelomacio.config;

import com.pelomacio.repository.LojaRepository;
import com.pelomacio.repository.UsuarioRepository;
import com.pelomacio.security.JwtAuthFilter;
import com.pelomacio.security.JwtService;
import com.pelomacio.security.LimiteDeRequisicoesFilter;
import com.pelomacio.security.PublicTenantFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UsuarioRepository usuarios,
                                           LojaRepository lojas)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // página pública de agendamento e cadastro de nova loja: sem login
                .requestMatchers("/api/publico/**").permitAll()
                // o Google redireciona o navegador para cá sem Authorization; o state assinado protege o fluxo
                .requestMatchers(HttpMethod.GET, "/api/google/callback").permitAll()
                // "Minha conta" (dados e troca de senha): qualquer usuário autenticado, inclusive CLIENTE
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/financeiro/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/profissionais/**", "/api/servicos/**", "/api/produtos/**")
                    .hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/profissionais/**", "/api/servicos/**", "/api/produtos/**", "/api/loja/**")
                    .hasRole("ADMINISTRADOR")
                // O perfil CLIENTE ainda não tem portal: não pode ler dados internos (tutores, pets, agenda...)
                .requestMatchers("/api/**").hasAnyRole("ADMINISTRADOR", "PROFISSIONAL")
                .anyRequest().permitAll()
            )
            .addFilterBefore(new LimiteDeRequisicoesFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new PublicTenantFilter(lojas), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthFilter(jwtService, usuarios), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${pelomacio.cors.allowed-origins}") String origens) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origens.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
