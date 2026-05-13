package ma.emsi.game_platform_backend.iam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  SESSION JWT - COMPARAISON APPROCHE MODERNE vs CLASSIQUE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : HttpSession (J2EE Servlet)                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. CRÉATION DE SESSION (Login Servlet) :
 *    ───────────────────────────────────────
 *    @WebServlet("/login")
 *    public class LoginServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            String email = request.getParameter("email");
 *            String password = request.getParameter("password");
 *
 *            // Vérification des credentials
 *            User user = userDAO.findByEmail(email);
 *            if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
 *                response.sendError(401, "Identifiants invalides");
 *                return;
 *            }
 *
 *            // CRÉATION DE SESSION SERVEUR (STATEFUL)
 *            HttpSession session = request.getSession(true); // Crée une session
 *            session.setAttribute("userId", user.getId());
 *            session.setAttribute("email", user.getEmail());
 *            session.setAttribute("role", user.getRole());
 *            session.setMaxInactiveInterval(30 * 60); // 30 minutes
 *
 *            // Le serveur génère un JSESSIONID et le place dans un cookie
 *            // Cookie: JSESSIONID=A1B2C3D4E5F6...
 *
 *            response.setStatus(200);
 *            response.getWriter().write("{\"message\": \"Login success\"}");
 *        }
 *    }
 *
 * 2. VÉRIFICATION DE SESSION (Filtre de Sécurité) :
 *    ───────────────────────────────────────────────
 *    @WebFilter("/*")
 *    public class AuthFilter implements Filter {
 *        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
 *            HttpServletRequest request = (HttpServletRequest) req;
 *            HttpServletResponse response = (HttpServletResponse) res;
 *
 *            String path = request.getRequestURI();
 *
 *            // Routes publiques (pas de vérification)
 *            if (path.startsWith("/login") || path.startsWith("/register")) {
 *                chain.doFilter(req, res);
 *                return;
 *            }
 *
 *            // RÉCUPÉRATION DE LA SESSION SERVEUR
 *            HttpSession session = request.getSession(false); // false = ne crée pas si absente
 *
 *            if (session == null || session.getAttribute("userId") == null) {
 *                response.sendError(401, "Non authentifié");
 *                return;
 *            }
 *
 *            // Vérification du rôle pour routes admin
 *            if (path.startsWith("/api/admin/")) {
 *                String role = (String) session.getAttribute("role");
 *                if (!"ADMIN".equals(role)) {
 *                    response.sendError(403, "Accès interdit");
 *                    return;
 *                }
 *            }
 *
 *            // Propagation de l'userId pour les servlets suivants
 *            request.setAttribute("currentUserId", session.getAttribute("userId"));
 *
 *            chain.doFilter(req, res);
 *        }
 *    }
 *
 * 3. LOGOUT (Invalidation de session) :
 *    ────────────────────────────────────
 *    @WebServlet("/logout")
 *    public class LogoutServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            HttpSession session = request.getSession(false);
 *            if (session != null) {
 *                session.invalidate(); // Détruit la session côté serveur
 *            }
 *            response.setStatus(200);
 *        }
 *    }
 *
 * 4. STOCKAGE DES SESSIONS (Serveur) :
 *    ────────────────────────────────────
 *    Les sessions sont stockées EN MÉMOIRE sur le serveur :
 *
 *    Map<String, HttpSession> sessions = new ConcurrentHashMap<>();
 *    // Clé : JSESSIONID (ex: "A1B2C3D4E5F6...")
 *    // Valeur : objet HttpSession contenant les attributs (userId, email, role)
 *
 *    → PROBLÈMES DE L'APPROCHE STATEFUL :
 *
 *      ❌ SCALABILITÉ :
 *         • Avec un Load Balancer + 3 serveurs (S1, S2, S3) :
 *           - User se connecte sur S1 → session stockée en mémoire de S1
 *           - Requête suivante arrive sur S2 → S2 n'a pas la session → 401
 *         • Solutions complexes :
 *           a) Sticky Sessions (coller l'utilisateur au même serveur)
 *              → Mauvaise distribution de charge
 *              → Si S1 tombe, toutes ses sessions sont perdues
 *           b) Session Replication (partage entre serveurs)
 *              → Overhead réseau important
 *              → Complexité de configuration
 *           c) Session Store centralisé (Redis, Memcached)
 *              → Infrastructure supplémentaire
 *              → Point de défaillance unique
 *
 *      ❌ CONSOMMATION MÉMOIRE :
 *         • 10 000 utilisateurs connectés = 10 000 objets HttpSession en RAM
 *         • Si chaque session = 5 KB → 50 MB de mémoire juste pour les sessions
 *         • Garbage Collection plus fréquent → baisse de performance
 *
 *      ❌ EXPIRATION :
 *         • Après 30 minutes d'inactivité, la session expire
 *         • L'utilisateur doit se reconnecter même si son activité était légitime
 *         • Pas de mécanisme de "refresh" transparent
 *
 *      ❌ API REST :
 *         • Les sessions violent le principe REST (stateless)
 *         • Impossible de consommer l'API depuis mobile natif (pas de cookies)
 *         • CORS complexe avec cookies (credentials: 'include')
 *
 *      ❌ SÉCURITÉ :
 *         • Vulnérable aux attaques CSRF (Cross-Site Request Forgery)
 *         • Cookie JSESSIONID transmis automatiquement → attaque possible
 *         • Solution : token CSRF additionnel (complexité++)
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE MODERNE : JWT (JSON Web Token) - STATELESS                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. STRUCTURE D'UN JWT :
 *    ─────────────────────
 *    Un JWT est composé de 3 parties séparées par des points :
 *
 *    HEADER.PAYLOAD.SIGNATURE
 *
 *    Exemple réel :
 *    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2NWY3YThjZDEyMzQ1Njc4OTBhYmNkZWYiLCJyb2xlIjoiVVNFUiIsImV4cCI6MTcxMDk1MjgwMH0.4sD5k3jK9mN2pQ8rT6vU7wX0yY1zA3bC5eF8gH9iJ0k
 *
 *    Décodage :
 *    ─────────
 *    HEADER (Base64URL décodé) :
 *    {
 *      "alg": "HS256",      ← Algorithme de signature (HMAC-SHA256)
 *      "typ": "JWT"         ← Type de token
 *    }
 *
 *    PAYLOAD (Base64URL décodé) :
 *    {
 *      "sub": "65f7a8cd1234567890abcdef",  ← Subject = userId
 *      "role": "USER",                      ← Claim custom
 *      "pseudo": "bilal",                   ← Claim custom
 *      "iat": 1710866400,                   ← Issued At (timestamp)
 *      "exp": 1710952800                    ← Expiration (timestamp)
 *    }
 *
 *    SIGNATURE (HMAC-SHA256) :
 *    HMACSHA256(
 *      base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *      SECRET_KEY
 *    )
 *
 *    → Le serveur peut VÉRIFIER l'authenticité sans lookup en base
 *    → Impossible de modifier le payload sans connaître la clé secrète
 *
 * 2. FLUX D'AUTHENTIFICATION JWT :
 *    ──────────────────────────────
 *
 *    LOGIN :
 *    ──────
 *    Client                          Serveur
 *      │                               │
 *      │ POST /api/auth/login          │
 *      │ { email, password }           │
 *      ├──────────────────────────────>│
 *      │                               │ 1. Vérification credentials
 *      │                               │ 2. Génération JWT :
 *      │                               │    - Claims : userId, role, exp
 *      │                               │    - Signature avec SECRET_KEY
 *      │                               │ 3. Sauvegarde en BD (blacklist)
 *      │                               │
 *      │ 200 OK                        │
 *      │ { token: "eyJhbG..." }        │
 *      │<──────────────────────────────┤
 *      │                               │
 *      │ Stockage dans localStorage    │
 *      │ ou sessionStorage             │
 *
 *    REQUÊTE AUTHENTIFIÉE :
 *    ─────────────────────
 *    Client                          Serveur
 *      │                               │
 *      │ GET /api/games                │
 *      │ Header:                       │
 *      │ Authorization: Bearer eyJ...  │
 *      ├──────────────────────────────>│
 *      │                               │ 1. Extraction du token
 *      │                               │ 2. Vérification signature
 *      │                               │ 3. Vérification expiration
 *      │                               │ 4. Check blacklist (optionnel)
 *      │                               │ 5. Extraction userId/role
 *      │                               │
 *      │ 200 OK                        │
 *      │ { games: [...] }              │
 *      │<──────────────────────────────┤
 *
 *    LOGOUT :
 *    ───────
 *    Client                          Serveur
 *      │                               │
 *      │ POST /api/auth/logout         │
 *      │ Header: Authorization: Bearer │
 *      ├──────────────────────────────>│
 *      │                               │ 1. Extraction du token
 *      │                               │ 2. Ajout à la blacklist
 *      │                               │    (Document Session isRevoked=true)
 *      │                               │
 *      │ 200 OK                        │
 *      │<──────────────────────────────┤
 *      │                               │
 *      │ Suppression du token          │
 *      │ côté client                   │
 *
 * 3. BLACKLIST / RÉVOCATION (Cette classe Session) :
 *    ───────────────────────────────────────────────
 *    Le JWT est STATELESS (aucune lookup nécessaire normalement).
 *    MAIS pour permettre le logout, on maintient une "blacklist" :
 *
 *    @Document(collection = "sessions")
 *    public class Session {
 *        private String jwtToken;      // Token complet
 *        private boolean isRevoked;    // true si logout
 *        private LocalDateTime expiresAt; // TTL automatique MongoDB
 *    }
 *
 *    Lors de la vérification du JWT (JwtAuthFilter) :
 *    ────────────────────────────────────────────────
 *    if (sessionRepository.existsByJwtTokenAndIsRevokedTrue(jwt)) {
 *        throw new AuthenticationException("Token révoqué");
 *    }
 *
 *    → Ce lookup est OPTIONNEL (choix de design)
 *    → Sans blacklist : un token reste valide jusqu'à son expiration
 *    → Avec blacklist : on peut révoquer instantanément (logout)
 *
 * 4. TTL INDEX MONGODB (Nettoyage automatique) :
 *    ──────────────────────────────────────────────
 *    @Indexed(expireAfterSeconds = 0)
 *    private LocalDateTime expiresAt;
 *
 *    → MongoDB supprime automatiquement les documents expirés
 *    → SANS : il faudrait un job cron pour nettoyer
 *
 *    Exemple : token généré le 20/03 à 10h00, expiration 24h
 *              expiresAt = 21/03 à 10h00
 *              → MongoDB supprime le document automatiquement le 21/03 à 10h00
 *              → Aucun code de nettoyage à écrire
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ COMPARAISON : HttpSession (STATEFUL) vs JWT (STATELESS)                    │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌──────────────────────┬────────────────────────┬────────────────────────────┐
 * │ CRITÈRE              │ HttpSession (Classique)│ JWT (Moderne)              │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Stockage             │ Serveur (RAM)          │ Client (localStorage)      │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Scalabilité          │ ❌ Difficile           │ ✅ Horizontale naturelle   │
 * │                      │ (sticky sessions/      │ (chaque serveur peut       │
 * │                      │  replication)          │  vérifier indépendamment)  │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Performance          │ ❌ Lookup en RAM       │ ✅ Aucun lookup (sauf      │
 * │                      │ à chaque requête       │ blacklist optionnelle)     │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Mémoire serveur      │ ❌ Proportionnelle     │ ✅ Constante (aucune)      │
 * │                      │ au nb d'users actifs   │                            │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Mobile natif         │ ❌ Problématique       │ ✅ Natif (HTTP header)     │
 * │                      │ (cookies compliqués)   │                            │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ API REST             │ ❌ Viole principe      │ ✅ Stateless               │
 * │                      │ stateless              │                            │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ CSRF                 │ ❌ Vulnérable          │ ✅ Immunisé (pas de cookie │
 * │                      │ (token CSRF requis)    │ auto-envoyé)               │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Logout immédiat      │ ✅ Natif               │ ⚠️ Nécessite blacklist     │
 * │                      │ (session.invalidate()) │ (optionnel)                │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Expiration           │ ❌ Fixe (timeout)      │ ✅ Flexible (exp claim)    │
 * │                      │                        │ + Refresh token possible   │
 * ├──────────────────────┼────────────────────────┼────────────────────────────┤
 * │ Microservices        │ ❌ Session partagée    │ ✅ Chaque service vérifie  │
 * │                      │ complexe               │ indépendamment             │
 * └──────────────────────┴────────────────────────┴────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ AVANTAGES DE L'APPROCHE JWT + SPRING SECURITY                              │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ✅ SCALABILITÉ :
 *    • Ajout de serveurs sans configuration (load balancer simple)
 *    • Pas de session replication
 *    • Pas de sticky sessions
 *
 * ✅ PERFORMANCE :
 *    • Pas de lookup en base à chaque requête (sauf blacklist optionnelle)
 *    • Vérification cryptographique rapide (HMAC)
 *    • Pas de consommation mémoire serveur
 *
 * ✅ FLEXIBILITÉ :
 *    • Claims personnalisés (role, permissions, etc.)
 *    • Expiration configurable
 *    • Refresh token pour renouvellement transparent
 *
 * ✅ SÉCURITÉ :
 *    • Immunisé contre CSRF (pas de cookie automatique)
 *    • Signature cryptographique (impossible de falsifier)
 *    • Expiration intégrée au token
 *
 * ✅ COMPATIBILITÉ :
 *    • API REST standard
 *    • Mobile natif (iOS, Android)
 *    • SPA (React, Angular, Vue)
 *    • Microservices
 *
 * ════════════════════════════════════════════════════════════════════════════════
 */
@Document(collection = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    private String id;

    /** FK vers User._id */
    private String userId;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * JWT_TOKEN - Token complet pour la blacklist
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING (Session HttpSession) :
     * ──────────────────────────────────
     *    Pas de stockage du token (le cookie JSESSIONID suffit)
     *    La session est stockée entièrement en mémoire serveur
     *
     * AVEC JWT :
     * ─────────
     *    Le token est stocké pour permettre la RÉVOCATION (blacklist).
     *
     *    @Indexed(unique = true) garantit qu'un token ne peut être enregistré
     *    qu'une seule fois, évitant les doublons si l'utilisateur se connecte
     *    plusieurs fois rapidement.
     *
     * USAGE :
     * ──────
     *    // Lors du logout
     *    Session session = sessionRepository.findByJwtToken(token);
     *    session.revoke();
     *    sessionRepository.save(session);
     *
     *    // Lors de la vérification (JwtAuthFilter)
     *    if (sessionRepository.existsByJwtTokenAndIsRevokedTrue(token)) {
     *        throw new AuthenticationException("Token révoqué");
     *    }
     */
    @Indexed(unique = true)
    private String jwtToken;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * TRACKING : IP Address et User-Agent
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Informations collectées lors du login pour :
     *    • Audit de sécurité (détection de connexions suspectes)
     *    • Gestion multi-device (afficher "connecté depuis Chrome, Windows 10")
     *    • Alerte si connexion depuis nouveau device/localisation
     *
     * SANS SPRING :
     * ────────────
     *    String ipAddress = request.getRemoteAddr();
     *    String userAgent = request.getHeader("User-Agent");
     *    // Stockage manuel dans la session ou en base
     *
     * AVEC SPRING :
     * ────────────
     *    // Dans AuthServiceImpl.login()
     *    String ipAddress = httpRequest.getRemoteAddr();
     *    String userAgent = httpRequest.getHeader("User-Agent");
     *
     *    Session session = Session.builder()
     *        .userId(user.getId())
     *        .jwtToken(token)
     *        .ipAddress(ipAddress)
     *        .userAgent(userAgent)
     *        .build();
     */
    private String ipAddress;
    private String userAgent;

    private LocalDateTime issuedAt;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * TTL INDEX - Suppression automatique par MongoDB
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING (Nettoyage manuel) :
     * ────────────────────────────────
     *    // Job cron exécuté toutes les heures
     *    @Scheduled(cron = "0 0 * * * *")
     *    public void cleanExpiredSessions() {
     *        LocalDateTime now = LocalDateTime.now();
     *        Bson filter = Filters.lt("expiresAt", now);
     *        sessionCollection.deleteMany(filter);
     *    }
     *
     *    → PROBLÈMES :
     *      • Code de nettoyage à écrire et maintenir
     *      • Consommation CPU périodique
     *      • Risque d'oubli dans les tests
     *
     * AVEC SPRING DATA MONGODB :
     * ─────────────────────────
     *    @Indexed(expireAfterSeconds = 0)
     *    private LocalDateTime expiresAt;
     *
     *    → MongoDB crée un index TTL (Time To Live)
     *    → Suppression automatique en arrière-plan par MongoDB
     *    → expireAfterSeconds = 0 signifie "supprimer à la date exacte expiresAt"
     *    → Aucun code Java nécessaire
     *
     * AVANTAGES :
     *    ✓ Nettoyage géré par MongoDB (pas de code Java)
     *    ✓ Performance optimale (index natif MongoDB)
     *    ✓ Pas de surcharge CPU applicative
     *    ✓ Fonctionne même si l'application est arrêtée
     */
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * IS_REVOKED - Blacklist pour logout instantané
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Un JWT est valide jusqu'à son expiration (claim "exp").
     * Problème : Comment révoquer un token avant son expiration (logout) ?
     *
     * SOLUTION 1 (Sans blacklist) :
     * ────────────────────────────
     *    • Pas de révocation immédiate possible
     *    • Le token reste valide jusqu'à expiration
     *    • Logout côté client uniquement (suppression du token)
     *    • Si un attaquant vole le token, il peut l'utiliser jusqu'à expiration
     *
     * SOLUTION 2 (Avec blacklist - Notre choix) :
     * ──────────────────────────────────────────
     *    • Lors du logout : isRevoked = true
     *    • Lors de chaque requête : vérification dans JwtAuthFilter
     *    • Si isRevoked = true → 401 Unauthorized
     *    • Sécurité renforcée : révocation instantanée
     *
     * COMPROMIS :
     * ──────────
     *    • On perd un peu de stateless (lookup en base)
     *    • Mais on gagne en sécurité (révocation immédiate)
     *    • Le lookup peut être optimisé avec un cache (Redis)
     */
    @Builder.Default
    private boolean isRevoked = false;

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTHODES MÉTIER
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Révoque le token (appelé lors du logout).
     */
    public void revoke() {
        this.isRevoked = true;
    }

    /**
     * Vérifie si le token est expiré.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}