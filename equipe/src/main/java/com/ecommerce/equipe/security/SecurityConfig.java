package com.ecommerce.equipe.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtRequestFilter jwtRequestFilter = new JwtRequestFilter(jwtUtil, userDetailsService);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // ========== ROTAS PÚBLICAS ==========
                        // Autenticação
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                        // ========== ROTAS DE PRODUTOS ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/produto/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/produto/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/produto/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/produto/**").hasAuthority("ADMIN")

                        // ========== ROTAS DE USUÁRIO ==========
                        // Listar todos os usuários - APENAS ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuario").hasAuthority("ADMIN")

                        // Ver perfil específico e imagem - USUÁRIO AUTENTICADO (será validado no controller)
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuario/*/imagem").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuario/*").authenticated()

                        // Atualizar perfil - USUÁRIO AUTENTICADO (será validado no controller)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/usuario/*").authenticated()

                        // Deletar usuário - APENAS ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/usuario/*").hasAuthority("ADMIN")

                        // ========== ROTAS DE PEDIDOS ==========
                        .requestMatchers("/api/v1/item-pedido/**").authenticated()
                        .requestMatchers("/api/v1/pedido/**").authenticated()

                        // ========== ROTAS DE AVALIAÇÃO ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/avaliacao/**").permitAll()
                        .requestMatchers("/api/v1/avaliacao/**").authenticated()

                        // ========== ROTAS DE ESTOQUE ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/estoque/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/estoque/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/estoque/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/estoque/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/estoque/**").hasAuthority("ADMIN")

                        // ========== ROTAS DE PAGAMENTO ==========
                        .requestMatchers("/api/v1/pagamento/**").authenticated()

                        // ========== OUTRAS ROTAS ==========
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token inválido ou ausente\"}");
                        })
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}