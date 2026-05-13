package ma.emsi.game_platform_backend.config;

import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.security.JwtAuthFilter;
import ma.emsi.game_platform_backend.iam.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // ═══════════════════════════════════════════════════════════
                // FIX IFRAME — X-Frame-Options
                // ═══════════════════════════════════════════════════════════
                // Problème : Spring Security ajoute X-Frame-Options: DENY par
                // défaut sur TOUTES les réponses, y compris /games/**.
                // Le navigateur refuse alors d'afficher le HTML dans l'iframe.
                //
                // SAMEORIGIN → autorise l'iframe si même host (localhost ici)
                // ═══════════════════════════════════════════════════════════
                // ✅ Autorise l'affichage dans une iframe depuis n'importe quelle origine
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                .authorizeHttpRequests(auth -> auth

                        // Public : Auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()

                        // Public : lecture catalogue (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()

                        // Public : fichiers statiques des jeux HTML5 servis par Spring Boot
                        .requestMatchers(HttpMethod.GET, "/games/**").permitAll()

                        // Admin : mutations jeux + upload ZIP
                        .requestMatchers(HttpMethod.POST,   "/api/games/**"          ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/games/**"          ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/games/**"          ).hasRole("ADMIN")
                        .requestMatchers("/api/admin/**"                             ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/games/upload"  ).hasRole("ADMIN")

                        // Premium
                        .requestMatchers("/api/games/premium/**").hasAnyRole("PREMIUM", "ADMIN")

                        // Authentifié
                        .requestMatchers("/api/scores/**"         ).authenticated()
                        .requestMatchers("/api/subscriptions/**"  ).authenticated()
                        .requestMatchers("/api/auth/me"           ).authenticated()
                        .requestMatchers("/api/auth/logout"       ).authenticated()
                        .requestMatchers("/api/gamification/**"   ).authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()

                        .anyRequest().authenticated()
                )

                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
