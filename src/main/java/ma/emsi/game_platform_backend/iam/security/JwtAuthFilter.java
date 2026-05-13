package ma.emsi.game_platform_backend.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.repository.SessionRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  JWT AUTH FILTER - COMPARAISON SPRING SECURITY vs FILTRE SERVLET MANUEL
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * RÔLE : Intercepter CHAQUE requête HTTP pour vérifier le JWT et authentifier l'utilisateur.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : Filtre Servlet Manuel (javax.servlet.Filter)          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. ENREGISTREMENT DU FILTRE (web.xml OU @WebFilter) :
 *    ────────────────────────────────────────────────────
 *
 *    A. Avec web.xml :
 *    ────────────────
 *    <?xml version="1.0" encoding="UTF-8"?>
 *    <web-app>
 *        <filter>
 *            <filter-name>JwtAuthFilter</filter-name>
 *            <filter-class>com.emsi.filters.JwtAuthFilter</filter-class>
 *        </filter>
 *        <filter-mapping>
 *            <filter-name>JwtAuthFilter</filter-name>
 *            <url-pattern>/*</url-pattern>  <!-- Toutes les requêtes -->
 *        </filter-mapping>
 *    </web-app>
 *
 *    B. Avec @WebFilter (Java EE 6+) :
 *    ─────────────────────────────────
 *    @WebFilter("/*")
 *    public class JwtAuthFilter implements Filter { }
 *
 *    → PROBLÈMES :
 *      • Pas d'intégration avec Spring Security
 *      • Pas de chaîne de filtres de sécurité
 *      • Difficile à tester unitairement
 *      • Pas de gestion centralisée de l'authentification
 *
 * 2. IMPLÉMENTATION DU FILTRE MANUEL (~150 lignes) :
 *    ────────────────────────────────────────────────
 *
 *    @WebFilter("/*")
 *    public class JwtAuthFilter implements Filter {
 *
 *        private JwtUtil jwtUtil;
 *        private UserDAO userDAO;
 *
 *        @Override
 *        public void init(FilterConfig filterConfig) throws ServletException {
 *            // Initialisation manuelle des dépendances
 *            this.jwtUtil = new JwtUtil("secret_key", 86400000);
 *            this.userDAO = new UserDAO();
 *        }
 *
 *        @Override
 *        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *                throws IOException, ServletException {
 *
 *            HttpServletRequest request = (HttpServletRequest) req;
 *            HttpServletResponse response = (HttpServletResponse) res;
 *
 *            String path = request.getRequestURI();
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 1. EXCLUSION DES ROUTES PUBLIQUES (Gestion manuelle)
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            // Routes publiques (pas de vérification JWT)
 *            if (path.equals("/api/auth/login") ||
 *                path.equals("/api/auth/register") ||
 *                path.equals("/api/auth/forgot-password") ||
 *                path.equals("/api/auth/reset-password") ||
 *                path.startsWith("/api/games") && request.getMethod().equals("GET")) {
 *
 *                chain.doFilter(req, res); // Passe au filtre/servlet suivant
 *                return;
 *            }
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 2. EXTRACTION DU TOKEN JWT
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            String authHeader = request.getHeader("Authorization");
 *
 *            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
 *                // Pas de token → 401 Unauthorized
 *                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *                response.setContentType("application/json");
 *                response.getWriter().write("{\"error\": \"Token manquant\"}");
 *                return; // Arrête la chaîne (ne pas appeler chain.doFilter)
 *            }
 *
 *            String jwt = authHeader.substring(7); // Retire "Bearer "
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 3. VALIDATION DU TOKEN JWT
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            String userId;
 *            try {
 *                // Vérification signature + expiration
 *                if (!jwtUtil.isTokenValid(jwt)) {
 *                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *                    response.setContentType("application/json");
 *                    response.getWriter().write("{\"error\": \"Token invalide\"}");
 *                    return;
 *                }
 *
 *                // Extraction du userId depuis le token
 *                userId = jwtUtil.extractUserId(jwt);
 *
 *            } catch (Exception e) {
 *                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *                response.setContentType("application/json");
 *                response.getWriter().write("{\"error\": \"Token invalide\"}");
 *                return;
 *            }
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 4. VÉRIFICATION BLACKLIST (Tokens révoqués - logout)
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            Connection conn = null;
 *            PreparedStatement ps = null;
 *            ResultSet rs = null;
 *
 *            try {
 *                conn = dataSource.getConnection();
 *
 *                String sql = "SELECT is_revoked FROM sessions WHERE jwt_token = ?";
 *                ps = conn.prepareStatement(sql);
 *                ps.setString(1, jwt);
 *                rs = ps.executeQuery();
 *
 *                if (rs.next() && rs.getBoolean("is_revoked")) {
 *                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *                    response.setContentType("application/json");
 *                    response.getWriter().write("{\"error\": \"Token révoqué\"}");
 *                    return;
 *                }
 *
 *            } catch (SQLException e) {
 *                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 *                response.getWriter().write("{\"error\": \"Erreur serveur\"}");
 *                return;
 *            } finally {
 *                // Fermeture des ressources (10 lignes)
 *                try {
 *                    if (rs != null) rs.close();
 *                    if (ps != null) ps.close();
 *                    if (conn != null) conn.close();
 *                } catch (SQLException e) {
 *                    e.printStackTrace();
 *                }
 *            }
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 5. CHARGEMENT DE L'UTILISATEUR DEPUIS LA BASE
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            User user = null;
 *            try {
 *                conn = dataSource.getConnection();
 *
 *                String sql = "SELECT * FROM users WHERE id = ?";
 *                ps = conn.prepareStatement(sql);
 *                ps.setString(1, userId);
 *                rs = ps.executeQuery();
 *
 *                if (!rs.next()) {
 *                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *                    response.getWriter().write("{\"error\": \"Utilisateur non trouvé\"}");
 *                    return;
 *                }
 *
 *                // Mapping manuel (15 lignes)
 *                user = new User();
 *                user.setId(rs.getString("id"));
 *                user.setEmail(rs.getString("email"));
 *                user.setRole(rs.getString("role"));
 *                user.setStatus(rs.getString("status"));
 *                // ... autres champs
 *
 *            } catch (SQLException e) {
 *                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 *                return;
 *            } finally {
 *                // Fermeture (10 lignes)
 *            }
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 6. VÉRIFICATION DU STATUT DU COMPTE
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            if ("SUSPENDED".equals(user.getStatus()) || "DELETED".equals(user.getStatus())) {
 *                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
 *                response.getWriter().write("{\"error\": \"Compte suspendu ou supprimé\"}");
 *                return;
 *            }
 *
 *            if ("LOCKED".equals(user.getStatus())) {
 *                response.setStatus(423); // 423 Locked
 *                response.getWriter().write("{\"error\": \"Compte verrouillé\"}");
 *                return;
 *            }
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 7. PROPAGATION DE L'UTILISATEUR POUR LES SERVLETS SUIVANTS
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            // Stockage dans la requête (accessible dans les servlets)
 *            request.setAttribute("currentUser", user);
 *            request.setAttribute("userId", user.getId());
 *            request.setAttribute("userRole", user.getRole());
 *
 *            // ══════════════════════════════════════════════════════════════════════
 *            // 8. CONTINUATION DE LA CHAÎNE
 *            // ══════════════════════════════════════════════════════════════════════
 *
 *            chain.doFilter(req, res);
 *        }
 *
 *        @Override
 *        public void destroy() {
 *            // Nettoyage si nécessaire
 *        }
 *    }
 *
 *    → BILAN FILTRE MANUEL :
 *      • ~150-200 lignes de code
 *      • Gestion manuelle des routes publiques (if/else répétitifs)
 *      • Gestion manuelle des erreurs HTTP (répétition de setStatus + getWriter)
 *      • Lookup JDBC pour la blacklist (~20 lignes)
 *      • Chargement user depuis la base (~30 lignes JDBC)
 *      • Pas d'intégration avec un système d'autorisation (rôles)
 *      • Propagation via request.setAttribute (pas type-safe)
 *
 * 3. UTILISATION DANS LES SERVLETS :
 *    ────────────────────────────────
 *
 *    @WebServlet("/api/games")
 *    public class GameServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            // Récupération de l'utilisateur depuis l'attribut
 *            User currentUser = (User) request.getAttribute("currentUser");
 *            String userRole = (String) request.getAttribute("userRole");
 *
 *            // Vérification manuelle du rôle
 *            if (!"ADMIN".equals(userRole)) {
 *                response.sendError(403, "Accès interdit");
 *                return;
 *            }
 *
 *            // Logique métier...
 *        }
 *    }
 *
 *    → PROBLÈMES :
 *      • Cast manuel (User) → risque ClassCastException
 *      • Vérification du rôle dupliquée dans CHAQUE servlet
 *      • Pas de gestion centralisée des autorisations
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING SECURITY : OncePerRequestFilter                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. CHAÎNE DE FILTRES SPRING SECURITY :
 *    ────────────────────────────────────
 *
 *    HTTP Request
 *      │
 *      ▼
 *    ┌──────────────────────────────────────────────────────────┐
 *    │  SecurityContextPersistenceFilter                        │
 *    │  • Initialise le SecurityContext                         │
 *    └────────────────────────┬─────────────────────────────────┘
 *                             │
 *                             ▼
 *    ┌──────────────────────────────────────────────────────────┐
 *    │  LogoutFilter                                            │
 *    │  • Gère les requêtes de logout                           │
 *    └────────────────────────┬─────────────────────────────────┘
 *                             │
 *                             ▼
 *    ┌──────────────────────────────────────────────────────────┐
 *    │  JwtAuthFilter  ← NOTRE FILTRE                           │
 *    │  • Extraction et validation du JWT                       │
 *    │  • Création du token d'authentification                  │
 *    │  • Placement dans le SecurityContext                     │
 *    └────────────────────────┬─────────────────────────────────┘
 *                             │
 *                             ▼
 *    ┌──────────────────────────────────────────────────────────┐
 *    │  ExceptionTranslationFilter                              │
 *    │  • Traduction des exceptions de sécurité                 │
 *    │  • AuthenticationException → 401                         │
 *    │  • AccessDeniedException → 403                           │
 *    └────────────────────────┬─────────────────────────────────┘
 *                             │
 *                             ▼
 *    ┌──────────────────────────────────────────────────────────┐
 *    │  FilterSecurityInterceptor                               │
 *    │  • Vérification des autorisations                        │
 *    │  • Basé sur la config SecurityConfig                     │
 *    │  • hasRole(), hasAuthority(), etc.                       │
 *    └────────────────────────┬─────────────────────────────────┘
 *                             │
 *                             ▼
 *    ┌──────────────────────────────────────────────────────────┐
 *    │  Controller (@RestController)                            │
 *    │  • Logique métier                                        │
 *    └──────────────────────────────────────────────────────────┘
 *
 * 2. ONCEPERREQUESTFILTER vs Filter :
 *    ─────────────────────────────────
 *
 *    Filter (javax.servlet.Filter) :
 *    ──────────────────────────────
 *      • Peut être exécuté plusieurs fois par requête
 *      • Si forward/include interne → exécution multiple
 *      • Problème : authentification dupliquée
 *
 *    OncePerRequestFilter (Spring) :
 *    ──────────────────────────────
 *      • Garantit UNE SEULE exécution par requête HTTP
 *      • Même en cas de forward/include
 *      • Optimisation : pas de re-authentification inutile
 *
 * 3. AVANTAGES DE L'APPROCHE SPRING SECURITY :
 *    ───────────────────────────────────────────
 *
 *    ✅ INTÉGRATION COMPLÈTE :
 *       • Intégré dans la chaîne de filtres Spring Security
 *       • Gestion automatique du SecurityContext
 *       • Support des annotations @PreAuthorize, @Secured
 *
 *    ✅ RÉDUCTION DU CODE :
 *       • ~60 lignes vs ~150-200 lignes (70% de réduction)
 *       • Pas de gestion manuelle des routes publiques (SecurityConfig)
 *       • Pas de gestion manuelle des status HTTP (Spring gère)
 *
 *    ✅ TYPE-SAFE :
 *       • SecurityContextHolder.getContext().getAuthentication()
 *       • @AuthenticationPrincipal dans les contrôleurs
 *       • Pas de cast manuel
 *
 *    ✅ TESTABILITÉ :
 *       • @WithMockUser dans les tests
 *       • Pas de dépendance à HttpServletRequest
 *
 *    ✅ EXTENSIBILITÉ :
 *       • Support LDAP, OAuth2, SAML sans modification
 *       • Chaîne de filtres configurable
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @Component : Enregistre le filtre comme un bean Spring.
 *              Spring Security l'ajoute automatiquement à la chaîne de filtres.
 *
 *              SANS @Component : Le filtre ne serait pas détecté par Spring.
 *              AVEC @Component : Spring l'ajoute automatiquement.
 *
 * OncePerRequestFilter : Classe abstraite Spring garantissant UNE SEULE exécution
 *                        par requête HTTP (même en cas de forward/include).
 *
 *                        SANS : Filter standard (risque d'exécution multiple).
 *                        AVEC : Optimisation automatique.
 *
 * @RequiredArgsConstructor : Génère le constructeur avec les dépendances final.
 *                            Spring injecte automatiquement.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * INJECTION DE DÉPENDANCES
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING (Filtre manuel) :
     * ─────────────────────────────
     *    public class JwtAuthFilter implements Filter {
     *        private JwtUtil jwtUtil;
     *        private UserDAO userDAO;
     *
     *        @Override
     *        public void init(FilterConfig config) {
     *            this.jwtUtil = new JwtUtil("secret", 86400000);
     *            this.userDAO = new UserDAO();
     *        }
     *    }
     *
     *    → Instanciation manuelle, couplage fort, tests difficiles
     *
     * AVEC SPRING :
     * ────────────
     *    @Component
     *    @RequiredArgsConstructor
     *    public class JwtAuthFilter {
     *        private final JwtUtil jwtUtil;              // Injecté par Spring
     *        private final UserDetailsServiceImpl userDetailsService;
     *        private final SessionRepository sessionRepository;
     *    }
     *
     *    → Injection automatique, découplage, testable facilement
     */
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final SessionRepository sessionRepository;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * DOFILTERINTERNAL - Logique du filtre (appelée UNE SEULE fois par requête)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * COMPARAISON SERVLET FILTER (~150 lignes) vs SPRING FILTER (~60 lignes) :
     * ────────────────────────────────────────────────────────────────────────
     *
     * Voir le bloc de commentaires au début du fichier pour le code complet
     * de l'approche Servlet manuelle.
     *
     * AVANTAGES SPRING :
     * ─────────────────
     * • Pas de gestion des routes publiques (SecurityConfig s'en charge)
     * • Pas de gestion manuelle des status HTTP (Spring gère)
     * • Pas de JDBC manuel pour blacklist (Spring Data)
     * • Pas de cast manuel (SecurityContext type-safe)
     * • Pas de répétition de response.setContentType + getWriter
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ══════════════════════════════════════════════════════════════════════
        // 1. EXTRACTION DU HEADER AUTHORIZATION
        // ══════════════════════════════════════════════════════════════════════
        //
        // Format attendu : Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
        //
        // SANS Spring Security (Servlet manuel) :
        //    Même code, mais suivi de 20+ lignes de gestion d'erreur manuelle
        //
        // AVEC Spring Security :
        //    • Si pas de token → on laisse passer
        //    • SecurityConfig rejettera la requête si route protégée
        //    • Gestion centralisée dans SecurityConfig (pas de if/else ici)
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token → on laisse passer
            // Spring Security refusera plus loin si route protégée
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7); // Retire "Bearer "

        // ══════════════════════════════════════════════════════════════════════
        // 2. VALIDATION DU FORMAT ET DE LA SIGNATURE JWT
        // ══════════════════════════════════════════════════════════════════════
        //
        // jwtUtil.isTokenValid(jwt) vérifie :
        //    • Format du token (3 parties séparées par des points)
        //    • Signature HMAC-SHA256 avec la clé secrète
        //    • Expiration (claim "exp")
        //
        // SANS Spring : 30+ lignes de try-catch + response.sendError
        // AVEC Spring : 3 lignes, Spring gère les erreurs via ExceptionTranslationFilter
        if (!jwtUtil.isTokenValid(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ══════════════════════════════════════════════════════════════════════
        // 3. VÉRIFICATION DE LA BLACKLIST (Tokens révoqués - logout)
        // ══════════════════════════════════════════════════════════════════════
        //
        // Un token peut être valide cryptographiquement mais révoqué (logout).
        //
        // SANS Spring (Servlet manuel + JDBC) :
        //    Connection conn = null;
        //    PreparedStatement ps = null;
        //    ResultSet rs = null;
        //    try {
        //        conn = dataSource.getConnection();
        //        ps = conn.prepareStatement("SELECT is_revoked FROM sessions WHERE jwt_token = ?");
        //        ps.setString(1, jwt);
        //        rs = ps.executeQuery();
        //        if (rs.next() && rs.getBoolean("is_revoked")) {
        //            response.sendError(401, "Token révoqué");
        //            return;
        //        }
        //    } catch (SQLException e) {
        //        response.sendError(500, "Erreur serveur");
        //        return;
        //    } finally {
        //        // Fermeture (10 lignes)
        //    }
        //
        //    → 30+ lignes
        //
        // AVEC Spring Data :
        //    if (sessionRepository.existsByJwtTokenAndIsRevokedTrue(jwt)) {
        //        response.setStatus(401);
        //        response.getWriter().write("{\"error\": \"Token révoqué\"}");
        //        return;
        //    }
        //
        //    → 5 lignes
        if (sessionRepository.existsByJwtTokenAndIsRevokedTrue(jwt)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token révoqué. Veuillez vous reconnecter.\"}");
            return;
        }

        // ══════════════════════════════════════════════════════════════════════
        // 4. EXTRACTION DU USERID ET CHARGEMENT DE L'UTILISATEUR
        // ══════════════════════════════════════════════════════════════════════
        //
        // SANS Spring (Servlet manuel + JDBC) :
        //    String userId = jwtUtil.extractUserId(jwt);
        //
        //    Connection conn = null;
        //    PreparedStatement ps = null;
        //    ResultSet rs = null;
        //    User user = null;
        //
        //    try {
        //        conn = dataSource.getConnection();
        //        ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
        //        ps.setString(1, userId);
        //        rs = ps.executeQuery();
        //
        //        if (!rs.next()) {
        //            response.sendError(401, "Utilisateur non trouvé");
        //            return;
        //        }
        //
        //        // Mapping manuel (15 lignes)
        //        user = new User();
        //        user.setId(rs.getString("id"));
        //        user.setEmail(rs.getString("email"));
        //        // ... 10+ lignes
        //
        //    } catch (SQLException e) {
        //        response.sendError(500);
        //        return;
        //    } finally {
        //        // Fermeture (10 lignes)
        //    }
        //
        //    → 40+ lignes
        //
        // AVEC Spring Security :
        //    String userId = jwtUtil.extractUserId(jwt);
        //    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
        //
        //    → 2 lignes
        //    → loadUserByUsername() encapsule tout :
        //      - Recherche en base (Spring Data : 1 ligne)
        //      - Mapping automatique (Spring Data)
        //      - Construction du UserDetails (Spring Security)
        final String userId = jwtUtil.extractUserId(jwt);

        // On ne charge l'utilisateur que si le SecurityContext est vide
        // (évite les chargements redondants pour une même requête)
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // loadUserByUsername() appelle userRepository.findByEmail()
            // et retourne un UserDetails (interface Spring Security)
            String role = jwtUtil.extractRole(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

            // ══════════════════════════════════════════════════════════════════
            // 5. CONSTRUCTION DU TOKEN D'AUTHENTIFICATION SPRING SECURITY
            // ══════════════════════════════════════════════════════════════════
            //
            // UsernamePasswordAuthenticationToken :
            //    • Représente une authentification réussie
            //    • Paramètres : (principal, credentials, authorities)
            //      - principal : UserDetails (contient email, rôle)
            //      - credentials : null (pas besoin après validation JWT)
            //      - authorities : List<GrantedAuthority> (ROLE_USER, ROLE_ADMIN...)
            //
            // SANS Spring Security (Servlet manuel) :
            //    request.setAttribute("currentUser", user);
            //    request.setAttribute("userId", user.getId());
            //    request.setAttribute("userRole", user.getRole());
            //
            //    → Propagation manuelle via attributs
            //    → Pas type-safe (cast manuel dans les servlets)
            //    → Risque de ClassCastException
            //
            // AVEC Spring Security :
            //    UsernamePasswordAuthenticationToken authToken = ...
            //    SecurityContextHolder.getContext().setAuthentication(authToken);
            //
            //    → Stockage dans le SecurityContext (ThreadLocal)
            //    → Type-safe
            //    → Accessible partout via SecurityContextHolder
            //    → Ou via @AuthenticationPrincipal dans les contrôleurs
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,                    // Principal (qui est connecté)
                            null,                          // Credentials (pas besoin)
                            userDetails.getAuthorities()   // Authorities (ROLE_USER, etc.)
                    );

            // Ajout des détails de la requête (IP, User-Agent, etc.)
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // ══════════════════════════════════════════════════════════════════
            // 6. STOCKAGE DANS LE SECURITYCONTEXT
            // ══════════════════════════════════════════════════════════════════
            //
            // SecurityContextHolder :
            //    • Utilise un ThreadLocal (une valeur par thread)
            //    • Accessible dans toute la stack de cette requête
            //    • Nettoyé automatiquement après la requête
            //
            // USAGE DANS LES CONTRÔLEURS :
            //
            //    // Méthode 1 : SecurityContextHolder (manuel)
            //    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            //    UserDetails userDetails = (UserDetails) auth.getPrincipal();
            //    String email = userDetails.getUsername();
            //
            //    // Méthode 2 : @AuthenticationPrincipal (recommandé)
            //    @GetMapping("/me")
            //    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
            //        String email = userDetails.getUsername();
            //        // ...
            //    }
            //
            // SANS Spring Security (Servlet manuel) :
            //    User user = (User) request.getAttribute("currentUser");
            //    if (user == null) {
            //        response.sendError(401);
            //        return;
            //    }
            //    String role = (String) request.getAttribute("userRole");
            //
            //    → Cast manuel, risque d'erreur, vérification null partout
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // ══════════════════════════════════════════════════════════════════════
        // 7. CONTINUATION DE LA CHAÎNE DE FILTRES
        // ══════════════════════════════════════════════════════════════════════
        //
        // À ce stade :
        //    • Le JWT est valide
        //    • L'utilisateur est chargé
        //    • Le SecurityContext contient l'authentification
        //
        // La requête continue vers :
        //    → ExceptionTranslationFilter (gestion erreurs de sécurité)
        //    → FilterSecurityInterceptor (vérification autorisations)
        //    → Controller (logique métier)
        filterChain.doFilter(request, response);
    }
}