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

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * SECURITY CONFIG - COMPARAISON SPRING SECURITY vs GESTION MANUELLE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * RÔLE : Configuration centralisée de la sécurité de l'application.
 * • Authentification (qui es-tu ?)
 * • Autorisation (que peux-tu faire ?)
 * • Protection CSRF, CORS, headers de sécurité
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : Gestion manuelle dans chaque Servlet/Filtre           │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. GESTION DES AUTORISATIONS (Répétée dans CHAQUE Servlet) :
 * ───────────────────────────────────────────────────────────
 *
 * @WebServlet("/api/admin/games")
 * public class AdminGameServlet extends HttpServlet {
 * protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 * // Vérification de l'authentification
 * HttpSession session = request.getSession(false);
 * if (session == null) {
 * response.sendError(401, "Non authentifié");
 * return;
 * }
 *
 * String role = (String) session.getAttribute("role");
 * if (role == null) {
 * response.sendError(401, "Non authentifié");
 * return;
 * }
 *
 * // Vérification du rôle ADMIN
 * if (!"ADMIN".equals(role)) {
 * response.sendError(403, "Accès interdit");
 * return;
 * }
 *
 * // Logique métier...
 * }
 * }
 *
 * @WebServlet("/api/games/premium")
 * public class PremiumGameServlet extends HttpServlet {
 * protected void doGet(HttpServletRequest request, HttpServletResponse response) {
 * // MÊME code de vérification répété (20 lignes)
 * HttpSession session = request.getSession(false);
 * if (session == null) {
 * response.sendError(401, "Non authentifié");
 * return;
 * }
 *
 * String role = (String) session.getAttribute("role");
 *
 * // Vérification du rôle PREMIUM ou ADMIN
 * if (!"PREMIUM".equals(role) && !"ADMIN".equals(role)) {
 * response.sendError(403, "Accès interdit");
 * return;
 * }
 *
 * // Logique métier...
 * }
 * }
 *
 * @WebServlet("/api/scores")
 * public class ScoreServlet extends HttpServlet {
 * protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 * // ENCORE le même code répété (20 lignes)
 * HttpSession session = request.getSession(false);
 * if (session == null) {
 * response.sendError(401, "Non authentifié");
 * return;
 * }
 * // ...
 * }
 * }
 *
 * → PROBLÈMES :
 * • Code dupliqué dans CHAQUE servlet (20+ lignes × 50 servlets = 1000+ lignes)
 * • Risque d'oubli (servlet sans vérification = faille de sécurité)
 * • Modification de la logique = modifier 50+ servlets
 * • Pas de vue d'ensemble de la sécurité de l'application
 * • Tests difficiles (dépendance à HttpSession)
 *
 * 2. GESTION DES ROUTES PUBLIQUES (Dans le filtre d'authentification) :
 * ────────────────────────────────────────────────────────────────────
 *
 * @WebFilter("/*")
 * public class AuthFilter implements Filter {
 * public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
 * HttpServletRequest request = (HttpServletRequest) req;
 * String path = request.getRequestURI();
 *
 * // Liste manuelle des routes publiques (30+ lignes)
 * if (path.equals("/api/auth/login") ||
 * path.equals("/api/auth/register") ||
 * path.equals("/api/auth/forgot-password") ||
 * path.equals("/api/auth/reset-password") ||
 * path.startsWith("/api/games") && request.getMethod().equals("GET") ||
 * path.startsWith("/games/") ||
 * path.equals("/") ||
 * path.startsWith("/static/") ||
 * path.startsWith("/css/") ||
 * path.startsWith("/js/") ||
 * path.startsWith("/images/") ||
 * path.endsWith(".html") ||
 * path.endsWith(".css") ||
 * path.endsWith(".js") ||
 * path.endsWith(".png") ||
 * path.endsWith(".jpg")) {
 *
 * chain.doFilter(req, res); // Route publique
 * return;
 * }
 *
 * // Vérification JWT pour les autres routes
 * // ...
 * }
 * }
 *
 * → PROBLÈMES :
 * • Configuration dispersée (filtre + servlets)
 * • If/else répétitifs et difficiles à lire
 * • Ajout d'une route publique = modifier le filtre
 * • Risque d'erreur (typo dans le chemin)
 * • Pas de gestion par méthode HTTP (GET/POST) claire
 *
 * 3. PROTECTION CSRF (Cross-Site Request Forgery) :
 * ───────────────────────────────────────────────
 *
 * SANS framework :
 * ───────────────
 * @WebServlet("/api/*")
 * public class CsrfFilter implements Filter {
 * public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
 * HttpServletRequest request = (HttpServletRequest) req;
 * HttpServletResponse response = (HttpServletResponse) res;
 *
 * // Génération du token CSRF lors du GET
 * if (request.getMethod().equals("GET")) {
 * String csrfToken = UUID.randomUUID().toString();
 * HttpSession session = request.getSession(true);
 * session.setAttribute("csrfToken", csrfToken);
 *
 * // Ajout dans un cookie ou header
 * Cookie cookie = new Cookie("XSRF-TOKEN", csrfToken);
 * cookie.setPath("/");
 * cookie.setHttpOnly(false); // Accessible en JS
 * response.addCookie(cookie);
 * }
 *
 * // Vérification lors du POST/PUT/DELETE
 * if (request.getMethod().equals("POST") ||
 * request.getMethod().equals("PUT") ||
 * request.getMethod().equals("DELETE")) {
 *
 * String csrfTokenFromHeader = request.getHeader("X-XSRF-TOKEN");
 * HttpSession session = request.getSession(false);
 * String csrfTokenFromSession = (String) session.getAttribute("csrfToken");
 *
 * if (csrfTokenFromHeader == null || !csrfTokenFromHeader.equals(csrfTokenFromSession)) {
 * response.sendError(403, "CSRF token invalide");
 * return;
 * }
 * }
 *
 * chain.doFilter(req, res);
 * }
 * }
 *
 * → PROBLÈMES :
 * • 30+ lignes de code à écrire et maintenir
 * • Risque d'oubli (un servlet sans protection)
 * • Gestion manuelle des cookies
 * • Incompatible avec les API REST stateless (JWT)
 *
 * 4. CORS (Cross-Origin Resource Sharing) :
 * ───────────────────────────────────────
 *
 * SANS framework :
 * ───────────────
 * @WebFilter("/*")
 * public class CorsFilter implements Filter {
 * public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
 * HttpServletResponse response = (HttpServletResponse) res;
 * HttpServletRequest request = (HttpServletRequest) req;
 *
 * // Configuration manuelle des headers CORS
 * response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
 * response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
 * response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
 * response.setHeader("Access-Control-Allow-Credentials", "true");
 * response.setHeader("Access-Control-Max-Age", "3600");
 *
 * // Gestion de la preflight request (OPTIONS)
 * if (request.getMethod().equals("OPTIONS")) {
 * response.setStatus(HttpServletResponse.SC_OK);
 * return;
 * }
 *
 * chain.doFilter(req, res);
 * }
 * }
 *
 * → PROBLÈMES :
 * • Headers hardcodés (pas de configuration par environnement)
 * • Gestion manuelle de la preflight request
 * • Risque de sécurité (Allow-Origin: * en production par erreur)
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING SECURITY : Configuration déclarative centralisée            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * AVANTAGES :
 * ──────────
 *
 * ✅ CONFIGURATION CENTRALISÉE :
 * • Un seul fichier (SecurityConfig.java)
 * • Vue d'ensemble claire de la sécurité
 * • Facile à auditer
 *
 * ✅ DÉCLARATIF vs IMPÉRATIF :
 * • SANS : if/else répétitifs dans chaque servlet
 * • AVEC : .requestMatchers("/api/admin/**").hasRole("ADMIN")
 *
 * ✅ PAS DE DUPLICATION :
 * • Règles définies une seule fois
 * • Appliquées automatiquement partout
 *
 * ✅ TYPE-SAFE :
 * • Erreur de compilation si route mal définie
 * • Auto-complétion dans l'IDE
 *
 * ✅ TESTABILITÉ :
 * • @WithMockUser pour tester les autorisations
 * • Pas de dépendance à HttpSession
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @Configuration : Indique que cette classe contient des définitions de beans Spring.
 * Spring scanne cette classe au démarrage et enregistre les @Bean.
 *
 * @EnableWebSecurity : Active Spring Security dans l'application.
 * Importe toutes les configurations par défaut de Spring Security.
 *
 * @EnableMethodSecurity : Active les annotations de sécurité au niveau des méthodes.
 * • @PreAuthorize("hasRole('ADMIN')")
 * • @Secured("ROLE_ADMIN")
 * • @RolesAllowed("ADMIN")
 *
 * @RequiredArgsConstructor : Génère le constructeur avec les dépendances final.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * INJECTION DE DÉPENDANCES
     * ════════════════════════════════════════════════════════════════════════════
     */
    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * CORS CONFIGURATION
     * ════════════════════════════════════════════════════════════════════════════
     *
     * CORS (Cross-Origin Resource Sharing) :
     * Permet au frontend (http://localhost:3000) d'appeler l'API backend
     * (http://localhost:8080) malgré la même politique d'origine (Same-Origin Policy).
     *
     * SANS SPRING (Filtre manuel - 20+ lignes) :
     * ──────────────────────────────────────────
     * @WebFilter("/*")
     * public class CorsFilter implements Filter {
     * public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
     * HttpServletResponse response = (HttpServletResponse) res;
     * response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
     * response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
     * response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
     * response.setHeader("Access-Control-Allow-Credentials", "true");
     *
     * if (request.getMethod().equals("OPTIONS")) {
     * response.setStatus(200);
     * return;
     * }
     * chain.doFilter(req, res);
     * }
     * }
     *
     * AVEC SPRING (Bean - 8 lignes) :
     * ────────────────────────────────
     */
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

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * SECURITY FILTER CHAIN - CONFIGURATION PRINCIPALE
     * ════════════════════════════════════════════════════════════════════════════
     *
     * COMPARAISON AVEC L'APPROCHE MANUELLE :
     * ──────────────────────────────────────
     *
     * SANS SPRING SECURITY (~500+ lignes dispersées dans 10+ filtres/servlets) :
     * ──────────────────────────────────────────────────────────────────────────
     * • AuthFilter.java (150 lignes) : Vérification JWT
     * • CorsFilter.java (30 lignes) : Gestion CORS
     * • CsrfFilter.java (40 lignes) : Protection CSRF
     * • AdminServlet.java (20 lignes) : Vérification rôle ADMIN
     * • PremiumServlet.java (20 lignes) : Vérification rôle PREMIUM
     * • ... 40+ servlets avec vérifications répétées
     *
     * Total : ~500-1000 lignes dispersées
     *
     * AVEC SPRING SECURITY (~50 lignes centralisées dans ce fichier) :
     * ───────────────────────────────────────────────────────────────
     * Configuration déclarative et centralisée ci-dessous.
     *
     * @Bean : Enregistre la méthode comme un bean Spring.
     * Spring Security utilise ce bean pour configurer la sécurité.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ══════════════════════════════════════════════════════════════════
                // CSRF (Cross-Site Request Forgery)
                // ══════════════════════════════════════════════════════════════════
                //
                // DÉSACTIVÉ car nous utilisons JWT (pas de cookies).
                //
                // AVEC cookies/session :
                //    • Activé par défaut dans Spring Security
                //    • Token CSRF requis pour POST/PUT/DELETE
                //    • Protection contre les attaques CSRF
                //
                // AVEC JWT :
                //    • Pas de cookies automatiques
                //    • Token dans le header Authorization
                //    • Immunisé contre CSRF naturellement
                //
                // SANS SPRING : 40+ lignes de code CSRF manuel à écrire
                // AVEC SPRING : 1 ligne pour désactiver
                .csrf(AbstractHttpConfigurer::disable)

                // ══════════════════════════════════════════════════════════════════
                // CORS (Cross-Origin Resource Sharing)
                // ══════════════════════════════════════════════════════════════════
                //
                // Applique la configuration CORS définie dans corsConfigurationSource()
                //
                // SANS SPRING : Filtre manuel + headers dans chaque response
                // AVEC SPRING : Applique automatiquement les headers CORS
                .cors(Customizer.withDefaults())

                // ══════════════════════════════════════════════════════════════════
                // X-FRAME-OPTIONS - Protection contre Clickjacking
                // ══════════════════════════════════════════════════════════════════
                //
                // PROBLÈME :
                //    Spring Security ajoute X-Frame-Options: DENY par défaut.
                //    → Empêche l'affichage des jeux HTML5 dans des iframes.
                //
                // SOLUTION :
                //    Désactiver frameOptions pour /games/**
                //
                // SANS SPRING : Pas de protection par défaut (faille de sécurité)
                // AVEC SPRING : Protection par défaut + configuration fine
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                // ══════════════════════════════════════════════════════════════════
                // AUTHORIZATION RULES - RÈGLES D'AUTORISATION
                // ══════════════════════════════════════════════════════════════════
                //
                // CONFIGURATION DÉCLARATIVE DES AUTORISATIONS :
                //    • .requestMatchers(...).permitAll()     → Routes publiques
                //    • .requestMatchers(...).hasRole(...)    → Routes protégées par rôle
                //    • .requestMatchers(...).authenticated() → Routes authentifiées (tout rôle)
                //    • .anyRequest().authenticated()         → Tout le reste = authentifié
                //
                // ORDRE D'ÉVALUATION :
                //    Spring Security applique la PREMIÈRE règle qui matche.
                //    → Les règles les plus SPÉCIFIQUES doivent être en PREMIER.
                //
                // EXEMPLE :
                //    .requestMatchers("/api/games/premium/**").hasRole("PREMIUM")  ← SPÉCIFIQUE
                //    .requestMatchers("/api/games/**").permitAll()                 ← GÉNÉRAL
                //
                //    Si on inverse :
                //    .requestMatchers("/api/games/**").permitAll()                 ← Matche en premier
                //    .requestMatchers("/api/games/premium/**").hasRole("PREMIUM")  ← Jamais atteint
                //
                //    → /api/games/premium serait PUBLIC (faille de sécurité)
                //
                // COMPARAISON AVEC L'APPROCHE MANUELLE :
                // ──────────────────────────────────────
                //
                // SANS SPRING (Répété dans chaque servlet - 20 lignes × 50 servlets) :
                //    @WebServlet("/api/admin/games")
                //    public class AdminGameServlet extends HttpServlet {
                //        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                //            HttpSession session = request.getSession(false);
                //            if (session == null) {
                //                response.sendError(401);
                //                return;
                //            }
                //            String role = (String) session.getAttribute("role");
                //            if (!"ADMIN".equals(role)) {
                //                response.sendError(403);
                //                return;
                //            }
                //            // Logique...
                //        }
                //    }
                //
                // AVEC SPRING (1 ligne par règle, centralisé) :
                //    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                //
                // → AVANTAGES :
                //    ✓ Configuration centralisée (vue d'ensemble)
                //    ✓ Pas de duplication (défini une fois, appliqué partout)
                //    ✓ Lisible et maintenable
                //    ✓ Testable avec @WithMockUser
                .authorizeHttpRequests(auth -> auth

                        // ──────────────────────────────────────────────────────
                        // ROUTES PUBLIQUES (pas d'authentification requise)
                        // ──────────────────────────────────────────────────────

                        // Auth endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()

                        // Catalogue de jeux (lecture seule)
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()

                        // Fichiers statiques des jeux HTML5
                        .requestMatchers(HttpMethod.GET, "/games/**").permitAll()

                        // ──────────────────────────────────────────────────────
                        // ROUTES ADMIN (rôle ADMIN requis)
                        // ──────────────────────────────────────────────────────

                        // Mutations sur les jeux (création/modification/suppression)
                        .requestMatchers(HttpMethod.POST,   "/api/games/**"          ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/games/**"          ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/games/**"          ).hasRole("ADMIN")

                        // Panel admin
                        .requestMatchers("/api/admin/**"                             ).hasRole("ADMIN")

                        // Upload de jeux (ZIP)
                        .requestMatchers(HttpMethod.POST, "/api/admin/games/upload"  ).hasRole("ADMIN")

                        // ──────────────────────────────────────────────────────
                        // ROUTES PREMIUM (rôles PREMIUM ou ADMIN)
                        // ──────────────────────────────────────────────────────

                        .requestMatchers("/api/games/premium/**").hasAnyRole("PREMIUM", "ADMIN")

                        // ──────────────────────────────────────────────────────
                        // ROUTES AUTHENTIFIÉES (tout utilisateur connecté)
                        // ──────────────────────────────────────────────────────

                        .requestMatchers("/api/scores/**"         ).authenticated()
                        .requestMatchers("/api/subscriptions/**"  ).authenticated()
                        .requestMatchers("/api/auth/me"           ).authenticated()
                        .requestMatchers("/api/auth/logout"       ).authenticated()
                        .requestMatchers("/api/gamification/**"   ).authenticated()

                        // Catégories publiques
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()

                        // Profils et dossiers statiques d'assets
                        .requestMatchers("/api/profile/**").authenticated()
                        .requestMatchers(
                                "/uploads/**",
                                "/avatars/**",
                                "/static/**"
                        ).permitAll()   // ← AJOUTE CECI

                        // ──────────────────────────────────────────────────────
                        // PAR DÉFAUT : Toute autre route nécessite authentification
                        // ──────────────────────────────────────────────────────

                        .anyRequest().authenticated()
                )

                // ══════════════════════════════════════════════════════════════════
                // SESSION MANAGEMENT - STATELESS
                // ══════════════════════════════════════════════════════════════════
                //
                // STATELESS : Pas de session HttpSession côté serveur.
                //             Chaque requête est indépendante (JWT dans le header).
                //
                // STATEFUL (par défaut avec Spring Security) :
                //    • Spring Security crée une session HttpSession
                //    • JSESSIONID dans un cookie
                //    • État stocké côté serveur
                //    → Incompatible avec JWT et architecture stateless
                //
                // AVEC JWT :
                //    SessionCreationPolicy.STATELESS
                //    → Spring Security ne crée JAMAIS de session
                //    → Pas de cookie JSESSIONID
                //    → Chaque requête contient le JWT
                //
                // SANS SPRING : Gestion manuelle (créer/valider session partout)
                // AVEC SPRING : 1 ligne de configuration
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ══════════════════════════════════════════════════════════════════
                // AUTHENTICATION PROVIDER
                // ══════════════════════════════════════════════════════════════════
                //
                // Fournisseur d'authentification utilisé par AuthenticationManager.
                //
                // SANS SPRING :
                //    User user = userDAO.findByEmail(email);
                //    if (!BCrypt.checkpw(password, user.getPassword())) {
                //        throw new Exception("Mauvais mot de passe");
                //    }
                //    → Répété dans chaque servlet de login
                //
                // AVEC SPRING :
                //    authenticationManager.authenticate(token);
                //    → Délègue au DaoAuthenticationProvider
                //    → Appelle UserDetailsService.loadUserByUsername()
                //    → Vérifie avec PasswordEncoder.matches()
                //    → Vérifie isEnabled(), isAccountNonLocked(), etc.
                .authenticationProvider(authenticationProvider())

                // ══════════════════════════════════════════════════════════════════
                // JWT FILTER - AJOUT DANS LA CHAÎNE
                // ══════════════════════════════════════════════════════════════════
                //
                // Ajoute notre JwtAuthFilter AVANT UsernamePasswordAuthenticationFilter
                // dans la chaîne de filtres Spring Security.
                //
                // CHAÎNE DE FILTRES :
                //    Request
                //      → SecurityContextPersistenceFilter
                //      → LogoutFilter
                //      → JwtAuthFilter  ← NOTRE FILTRE (ajouté ici)
                //      → UsernamePasswordAuthenticationFilter (désactivé en STATELESS)
                //      → ExceptionTranslationFilter
                //      → FilterSecurityInterceptor
                //      → Controller
                //
                // SANS SPRING : Enregistrement manuel dans web.xml + ordre incertain
                // AVEC SPRING : Insertion précise dans la chaîne
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * PASSWORD ENCODER - HACHAGE BCRYPT
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Bean Spring pour le hachage des mots de passe avec BCrypt.
     *
     * SANS SPRING :
     * ────────────
     * // Dans chaque servlet où on gère les mots de passe
     * String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
     *
     * // Vérification
     * boolean matches = BCrypt.checkpw(rawPassword, hashedPassword);
     *
     * → Duplication du code
     * → Nombre de rounds (12) hardcodé partout
     * → Difficile de changer d'algorithme (Argon2, PBKDF2)
     *
     * AVEC SPRING :
     * ────────────
     * @Bean
     * public PasswordEncoder passwordEncoder() {
     * return new BCryptPasswordEncoder(12);
     * }
     *
     * // Injection dans les services
     * passwordEncoder.encode(rawPassword);
     * passwordEncoder.matches(rawPassword, encodedPassword);
     *
     * → Configuration centralisée
     * → Facile de changer d'algorithme (1 ligne à modifier)
     * → Interface PasswordEncoder (découplage)
     *
     * BCryptPasswordEncoder(12) :
     * • 12 rounds = 2^12 = 4096 itérations
     * • Plus le nombre est élevé, plus c'est sécurisé mais lent
     * • Recommandation : 12 (compromis sécurité/performance)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * AUTHENTICATION PROVIDER - DAO AUTHENTICATION
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Fournisseur d'authentification qui utilise UserDetailsService et PasswordEncoder.
     *
     * SANS SPRING :
     * ────────────
     * User user = userDAO.findByEmail(email);
     * if (user == null) throw new Exception("User not found");
     * if (!BCrypt.checkpw(password, user.getPassword())) {
     * throw new Exception("Bad password");
     * }
     * if ("SUSPENDED".equals(user.getStatus())) {
     * throw new Exception("Account suspended");
     * }
     * // ... autres vérifications
     *
     * → 30+ lignes répétées dans chaque servlet de login
     *
     * AVEC SPRING :
     * ────────────
     * DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
     * provider.setUserDetailsService(userDetailsService);
     * provider.setPasswordEncoder(passwordEncoder);
     *
     * // Utilisation
     * authenticationManager.authenticate(
     * new UsernamePasswordAuthenticationToken(email, password)
     * );
     *
     * → Spring Security appelle automatiquement :
     * 1. userDetailsService.loadUserByUsername(email)
     * 2. passwordEncoder.matches(password, user.getPassword())
     * 3. Vérifie isEnabled(), isAccountNonLocked(), etc.
     * 4. Lance des exceptions typées (BadCredentialsException, etc.)
     *
     * → 1 ligne vs 30+ lignes manuelles
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * AUTHENTICATION MANAGER
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Point d'entrée pour l'authentification dans Spring Security.
     *
     * USAGE :
     * ──────
     * @Service
     * public class AuthServiceImpl {
     * private final AuthenticationManager authenticationManager;
     *
     * public void login(LoginRequest request) {
     * authenticationManager.authenticate(
     * new UsernamePasswordAuthenticationToken(email, password)
     * );
     * }
     * }
     *
     * SANS SPRING : Aucun point d'entrée centralisé, logique dispersée
     * AVEC SPRING : AuthenticationManager → délègue au provider → UserDetailsService
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}