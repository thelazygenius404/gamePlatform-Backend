package ma.emsi.game_platform_backend.iam.serviceImpl;

import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.model.*;
import ma.emsi.game_platform_backend.shared.enums.*;
import ma.emsi.game_platform_backend.iam.dto.*;
import ma.emsi.game_platform_backend.iam.repository.*;
import ma.emsi.game_platform_backend.iam.security.JwtUtil;
import ma.emsi.game_platform_backend.iam.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  AUTH SERVICE - COMPARAISON SPRING vs APPROCHE SERVLET CLASSIQUE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : Logique dans les Servlets (J2EE)                      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Dans l'approche J2EE classique, TOUTE la logique métier est écrite directement
 * dans les Servlets. Il n'y a PAS de couche Service séparée.
 *
 * PROBLÈMES DE L'APPROCHE SERVLET MONOLITHIQUE :
 * ───────────────────────────────────────────────
 *
 * 1. DUPLICATION DE CODE :
 *    ────────────────────
 *    // RegisterServlet.java (~150 lignes)
 *    @WebServlet("/register")
 *    public class RegisterServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            // 1. Récupération des paramètres
 *            String email = request.getParameter("email");
 *            String password = request.getParameter("password");
 *            String pseudo = request.getParameter("pseudo");
 *
 *            // 2. Validation manuelle (30+ lignes)
 *            if (email == null || email.isEmpty()) {
 *                response.sendError(400, "Email requis");
 *                return;
 *            }
 *            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
 *                response.sendError(400, "Email invalide");
 *                return;
 *            }
 *            if (password == null || password.length() < 8) {
 *                response.sendError(400, "Mot de passe trop court");
 *                return;
 *            }
 *            // ... 20 lignes de validation supplémentaires
 *
 *            // 3. Vérification unicité email (20 lignes)
 *            Connection conn = null;
 *            PreparedStatement ps = null;
 *            ResultSet rs = null;
 *            try {
 *                conn = dataSource.getConnection();
 *                ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
 *                ps.setString(1, email);
 *                rs = ps.executeQuery();
 *                if (rs.next()) {
 *                    response.sendError(400, "Email déjà utilisé");
 *                    return;
 *                }
 *            } catch (SQLException e) {
 *                response.sendError(500, "Erreur base de données");
 *                return;
 *            } finally {
 *                // Fermeture ressources (10 lignes)
 *            }
 *
 *            // 4. Hachage du mot de passe (5 lignes)
 *            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
 *
 *            // 5. Insertion en base (30 lignes)
 *            try {
 *                conn = dataSource.getConnection();
 *                String sql = "INSERT INTO users (id, email, password, pseudo, role, status, " +
 *                           "failed_attempts, points, level, created_at, updated_at) " +
 *                           "VALUES (?, ?, ?, ?, 'USER', 'ACTIVE', 0, 0, 1, NOW(), NOW())";
 *                ps = conn.prepareStatement(sql);
 *                ps.setString(1, UUID.randomUUID().toString());
 *                ps.setString(2, email);
 *                ps.setString(3, hashedPassword);
 *                ps.setString(4, pseudo);
 *                ps.executeUpdate();
 *            } catch (SQLException e) {
 *                response.sendError(500, "Erreur insertion");
 *                return;
 *            } finally {
 *                // Fermeture (10 lignes)
 *            }
 *
 *            // 6. Création de session (10 lignes)
 *            HttpSession session = request.getSession(true);
 *            session.setAttribute("email", email);
 *            session.setAttribute("role", "USER");
 *
 *            // 7. Réponse JSON manuelle (15 lignes)
 *            response.setContentType("application/json");
 *            response.setCharacterEncoding("UTF-8");
 *            PrintWriter out = response.getWriter();
 *            out.write("{\"message\": \"Inscription réussie\"}");
 *        }
 *    }
 *
 *    // LoginServlet.java (~200 lignes)
 *    @WebServlet("/login")
 *    public class LoginServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            // MÊME logique de validation répétée (30 lignes)
 *            // MÊME logique de connexion DB répétée (20 lignes)
 *            // MÊME logique de gestion erreur répétée (30 lignes)
 *            // + Logique spécifique login (100 lignes)
 *        }
 *    }
 *
 *    → PROBLÈME : Duplication massive entre RegisterServlet et LoginServlet
 *    → MAINTENANCE : Modifier la validation = modifier 5-6 servlets
 *
 * 2. COUPLAGE FORT AVEC HTTP :
 *    ─────────────────────────
 *    • Impossible de tester la logique métier sans HttpServletRequest/Response
 *    • Impossible de réutiliser le code depuis un job batch ou une API gRPC
 *    • Logique métier mélangée avec le code HTTP (sendError, getParameter, etc.)
 *
 * 3. PAS DE SÉPARATION DES RESPONSABILITÉS :
 *    ───────────────────────────────────────
 *    Un Servlet fait TOUT :
 *      • Validation des données
 *      • Accès base de données
 *      • Logique métier
 *      • Gestion des exceptions
 *      • Sérialisation JSON
 *      • Gestion des sessions
 *
 *    → Violation du principe SRP (Single Responsibility Principle)
 *    → Code difficile à tester unitairement
 *    → Code difficile à maintenir (500+ lignes par servlet)
 *
 * 4. GESTION DES TRANSACTIONS MANUELLE :
 *    ────────────────────────────────────
 *    try {
 *        conn.setAutoCommit(false);
 *
 *        // Opération 1
 *        ps1 = conn.prepareStatement("INSERT INTO users ...");
 *        ps1.executeUpdate();
 *
 *        // Opération 2
 *        ps2 = conn.prepareStatement("INSERT INTO sessions ...");
 *        ps2.executeUpdate();
 *
 *        conn.commit();
 *    } catch (SQLException e) {
 *        if (conn != null) {
 *            conn.rollback();
 *        }
 *        throw e;
 *    } finally {
 *        // Fermeture ressources
 *    }
 *
 *    → 30+ lignes de code transactionnel répété partout
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING : COUCHE SERVICE (Séparation des responsabilités)          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ARCHITECTURE EN COUCHES :
 * ────────────────────────
 *
 *    ┌─────────────────────────────────────────────────────┐
 *    │            HTTP REQUEST                             │
 *    │    (JSON : { "email", "password", "pseudo" })       │
 *    └──────────────────────┬──────────────────────────────┘
 *                           │
 *                           ▼
 *    ┌─────────────────────────────────────────────────────┐
 *    │       CONTROLLER (@RestController)                  │
 *    │   • Réception HTTP                                  │
 *    │   • Validation (@Valid)                             │
 *    │   • Appel du service                                │
 *    │   • Sérialisation JSON automatique                  │
 *    └──────────────────────┬──────────────────────────────┘
 *                           │
 *                           ▼
 *    ┌─────────────────────────────────────────────────────┐
 *    │          SERVICE (@Service)  ← NOUS SOMMES ICI      │
 *    │   • Logique métier PURE                             │
 *    │   • Orchestration des opérations                    │
 *    │   • Transactions (@Transactional)                   │
 *    │   • Pas de dépendance HTTP                          │
 *    └──────────────────────┬──────────────────────────────┘
 *                           │
 *                           ▼
 *    ┌─────────────────────────────────────────────────────┐
 *    │       REPOSITORY (@Repository)                      │
 *    │   • Accès base de données                           │
 *    │   • Requêtes générées automatiquement               │
 *    │   • Mapping automatique                             │
 *    └──────────────────────┬──────────────────────────────┘
 *                           │
 *                           ▼
 *    ┌─────────────────────────────────────────────────────┐
 *    │          BASE DE DONNÉES MongoDB                    │
 *    └─────────────────────────────────────────────────────┘
 *
 * AVANTAGES DE LA COUCHE SERVICE :
 * ────────────────────────────────
 *
 * ✅ RÉUTILISABILITÉ :
 *    • La logique métier peut être appelée depuis :
 *      - Un contrôleur REST (@RestController)
 *      - Un contrôleur GraphQL
 *      - Un job batch (@Scheduled)
 *      - Un consumer Kafka
 *      - Un test unitaire
 *
 * ✅ TESTABILITÉ :
 *    • Tests unitaires SANS serveur HTTP
 *    • Injection de mocks pour les repositories
 *    • Pas de dépendance à HttpServletRequest/Response
 *
 *    @Test
 *    void testRegister() {
 *        // Arrange
 *        when(userRepository.existsByEmail("test@emsi.ma")).thenReturn(false);
 *
 *        RegisterRequest request = new RegisterRequest("test@emsi.ma", "Pass@123", "bilal");
 *
 *        // Act
 *        AuthResponse response = authService.register(request);
 *
 *        // Assert
 *        assertNotNull(response.getToken());
 *        verify(userRepository).save(any(User.class));
 *    }
 *
 * ✅ MAINTENABILITÉ :
 *    • Logique métier centralisée (pas de duplication)
 *    • Modification de la validation = 1 seul endroit
 *    • Code clair et lisible (~50 lignes par méthode vs 200 dans un servlet)
 *
 * ✅ TRANSACTIONS DÉCLARATIVES :
 *    @Transactional
 *    public void monService() {
 *        userRepository.save(user);
 *        sessionRepository.save(session);
 *        // Commit automatique ou rollback si exception
 *    }
 *
 *    → 1 annotation vs 30+ lignes de code transactionnel
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @Service : Annotation Spring qui marque cette classe comme un bean de service.
 *            Spring l'enregistre dans l'IoC Container et gère son cycle de vie.
 *
 *            SANS @Service :
 *              AuthService service = new AuthServiceImpl(
 *                  new UserDAO(), new SessionDAO(), new JwtUtil(), ...
 *              );
 *              → Instanciation manuelle partout
 *              → Couplage fort
 *              → Tests difficiles
 *
 *            AVEC @Service :
 *              @Autowired private AuthService authService;
 *              → Spring injecte automatiquement
 *              → Découplage total
 *              → Testable facilement (injection de mocks)
 *
 * @RequiredArgsConstructor : Annotation Lombok qui génère un constructeur avec tous
 *                            les champs final. Spring utilise ce constructeur pour
 *                            l'injection de dépendances.
 *
 *                            Code généré par Lombok :
 *                            public AuthServiceImpl(
 *                                UserRepository userRepository,
 *                                SessionRepository sessionRepository,
 *                                PasswordResetTokenRepository resetTokenRepository,
 *                                PasswordEncoder passwordEncoder,
 *                                JwtUtil jwtUtil,
 *                                AuthenticationManager authenticationManager
 *                            ) {
 *                                this.userRepository = userRepository;
 *                                this.sessionRepository = sessionRepository;
 *                                // ... etc
 *                            }
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * INJECTION DE DÉPENDANCES (Dependency Injection)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING (Instanciation manuelle) :
     * ──────────────────────────────────────
     *    public class AuthServiceImpl {
     *        private UserDAO userDAO;
     *        private SessionDAO sessionDAO;
     *        private PasswordEncoder passwordEncoder;
     *        private JwtUtil jwtUtil;
     *
     *        public AuthServiceImpl() {
     *            // Instanciation manuelle des dépendances
     *            this.userDAO = new UserDAO();
     *            this.sessionDAO = new SessionDAO();
     *            this.passwordEncoder = new BCryptPasswordEncoder();
     *            this.jwtUtil = new JwtUtil("secret_key", 86400000);
     *        }
     *
     *        // OU via setters
     *        public void setUserDAO(UserDAO userDAO) {
     *            this.userDAO = userDAO;
     *        }
     *    }
     *
     *    → PROBLÈMES :
     *      • Couplage fort (dépendance à l'implémentation concrète)
     *      • Difficile à tester (impossible d'injecter des mocks)
     *      • Configuration hardcodée ("secret_key", 86400000)
     *      • Gestion manuelle du cycle de vie
     *
     * AVEC SPRING (Constructor Injection) :
     * ─────────────────────────────────────
     *    @Service
     *    @RequiredArgsConstructor // Génère le constructeur automatiquement
     *    public class AuthServiceImpl {
     *        private final UserRepository userRepository;      // Injecté par Spring
     *        private final SessionRepository sessionRepository;
     *        private final PasswordEncoder passwordEncoder;
     *        private final JwtUtil jwtUtil;
     *
     *        // Constructeur généré par Lombok + utilisé par Spring pour DI
     *    }
     *
     *    → AVANTAGES :
     *      ✓ Découplage (dépendance à l'interface)
     *      ✓ Testable (injection de mocks facile)
     *      ✓ Configuration externalisée (application.yml)
     *      ✓ Cycle de vie géré par Spring
     *      ✓ Singleton par défaut (une seule instance partagée)
     *
     * POURQUOI final ?
     * ───────────────
     *    • Garantit que les dépendances sont initialisées UNE SEULE fois
     *    • Évite les modifications accidentelles
     *    • Nécessaire pour @RequiredArgsConstructor
     *    • Best practice : immutabilité des dépendances
     */
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * INJECTION DE CONFIGURATION (@Value)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING (Configuration hardcodée) :
     * ───────────────────────────────────────
     *    public class AuthServiceImpl {
     *        private int maxFailedAttempts = 5;             // Hardcodé
     *        private int lockDurationMinutes = 30;          // Hardcodé
     *        private long jwtExpirationMs = 86400000;       // Hardcodé
     *
     *        // OU lecture manuelle de fichier properties
     *        static {
     *            try {
     *                Properties props = new Properties();
     *                props.load(new FileInputStream("config.properties"));
     *                maxFailedAttempts = Integer.parseInt(props.getProperty("max.attempts"));
     *            } catch (IOException e) {
     *                // Gérer l'erreur
     *            }
     *        }
     *    }
     *
     *    → PROBLÈMES :
     *      • Valeurs hardcodées dans le code
     *      • Pas de configuration par environnement (dev/prod)
     *      • Gestion manuelle des erreurs de parsing
     *      • Recompilation nécessaire pour changer une valeur
     *
     * AVEC SPRING (@Value) :
     * ─────────────────────
     *    @Value("${app.security.max-failed-attempts}")
     *    private int maxFailedAttempts;
     *
     *    Fichier application.yml :
     *    app:
     *      security:
     *        max-failed-attempts: 5
     *        lock-duration-minutes: 30
     *
     *    Fichier application-prod.yml (override pour production) :
     *    app:
     *      security:
     *        max-failed-attempts: 3      ← Plus strict en prod
     *        lock-duration-minutes: 60   ← Verrouillage plus long
     *
     *    → AVANTAGES :
     *      ✓ Configuration externalisée (pas dans le code)
     *      ✓ Configuration par environnement (dev/test/prod)
     *      ✓ Validation automatique (erreur au démarrage si manquant)
     *      ✓ Conversion de type automatique (String → int)
     *      ✓ Aucune recompilation pour changer une valeur
     *      ✓ Support des valeurs par défaut : @Value("${max:5}")
     */
    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * REGISTER - Inscription d'un nouvel utilisateur
     * ════════════════════════════════════════════════════════════════════════════
     *
     * COMPARAISON SERVLET vs SERVICE SPRING :
     * ───────────────────────────────────────
     *
     * SERVLET CLASSIQUE (~150 lignes) :
     * ─────────────────────────────────
     *    @WebServlet("/register")
     *    public class RegisterServlet extends HttpServlet {
     *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
     *            // 1. Récupération paramètres (10 lignes)
     *            String email = request.getParameter("email");
     *            String password = request.getParameter("password");
     *            String pseudo = request.getParameter("pseudo");
     *
     *            // 2. Validation manuelle (40 lignes)
     *            if (email == null || email.isEmpty()) {
     *                response.sendError(400, "Email requis");
     *                return;
     *            }
     *            // ... 35 lignes de validation
     *
     *            // 3. Vérification unicité (25 lignes JDBC)
     *            Connection conn = null;
     *            PreparedStatement ps = null;
     *            ResultSet rs = null;
     *            try {
     *                conn = dataSource.getConnection();
     *                ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
     *                ps.setString(1, email);
     *                rs = ps.executeQuery();
     *                if (rs.next()) {
     *                    response.sendError(400, "Email déjà utilisé");
     *                    return;
     *                }
     *            } finally {
     *                // Fermeture (10 lignes)
     *            }
     *
     *            // 4. Hachage BCrypt (5 lignes)
     *            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
     *
     *            // 5. Insertion (35 lignes JDBC)
     *            try {
     *                conn = dataSource.getConnection();
     *                String sql = "INSERT INTO users ...";
     *                ps = conn.prepareStatement(sql);
     *                // 15 lignes de setString/setInt
     *                ps.executeUpdate();
     *            } finally {
     *                // Fermeture (10 lignes)
     *            }
     *
     *            // 6. Réponse JSON (10 lignes)
     *            response.setContentType("application/json");
     *            PrintWriter out = response.getWriter();
     *            out.write("{\"message\": \"OK\"}");
     *        }
     *    }
     *
     *    Total : ~150 lignes
     *
     * SERVICE SPRING (~25 lignes) :
     * ────────────────────────────
     *    @Override
     *    public AuthResponse register(RegisterRequest request) {
     *        // Validation : déjà faite par @Valid dans le controller (0 lignes ici)
     *
     *        // Vérification unicité (1 ligne)
     *        if (userRepository.existsByEmail(request.getEmail())) {
     *            throw new IllegalArgumentException("Email déjà utilisé");
     *        }
     *
     *        // Création user (10 lignes avec Builder)
     *        User user = User.builder()
     *            .email(request.getEmail())
     *            .password(passwordEncoder.encode(request.getPassword()))
     *            .pseudo(request.getPseudo())
     *            .role(Role.USER)
     *            .status(AccountStatus.ACTIVE)
     *            .failedAttempts(0)
     *            .points(0)
     *            .level(1)
     *            .createdAt(LocalDateTime.now())
     *            .updatedAt(LocalDateTime.now())
     *            .build();
     *
     *        // Sauvegarde (1 ligne)
     *        User savedUser = userRepository.save(user);
     *
     *        // Génération JWT (1 ligne)
     *        String token = generateAndPersistToken(savedUser, null, null);
     *
     *        // Retour DTO (8 lignes)
     *        return AuthResponse.builder()
     *            .token(token)
     *            .userId(savedUser.getId())
     *            .email(savedUser.getEmail())
     *            .pseudo(savedUser.getPseudo())
     *            .role(savedUser.getRole())
     *            .expiresIn(jwtExpirationMs)
     *            .build();
     *    }
     *
     *    Total : ~25 lignes
     *    Réduction : 83% de code en moins
     */
    @Override
    public AuthResponse register(RegisterRequest request) {

        // Vérification de l'unicité de l'email (RG-01)
        // Spring Data : 1 ligne vs 25 lignes JDBC
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        // Vérification de l'unicité du pseudo
        if (userRepository.existsByPseudo(request.getPseudo())) {
            throw new IllegalArgumentException("Ce pseudo est déjà pris.");
        }

        // Construction du document User avec le pattern Builder (Lombok)
        // SANS Builder : 12 lignes de setters
        // AVEC Builder : 10 lignes fluides et lisibles
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt automatique
                .pseudo(request.getPseudo())
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .failedAttempts(0)
                .points(0)
                .level(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Sauvegarde en base
        // Spring Data MongoDB : 1 ligne vs 35 lignes JDBC
        User savedUser = userRepository.save(user);

        // Génération du JWT pour connexion directe après inscription
        String token = generateAndPersistToken(savedUser, null, null);

        // Construction de la réponse DTO
        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .pseudo(savedUser.getPseudo())
                .role(savedUser.getRole())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LOGIN - Authentification avec gestion du brute force (RG-03)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * RÈGLE MÉTIER RG-03 : Après 5 échecs de connexion, le compte est verrouillé 30 min.
     *
     * COMPARAISON SERVLET vs SERVICE SPRING :
     * ───────────────────────────────────────
     *
     * SERVLET CLASSIQUE (~200 lignes) :
     * ─────────────────────────────────
     *    @WebServlet("/login")
     *    public class LoginServlet extends HttpServlet {
     *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
     *            // 1. Récupération paramètres (10 lignes)
     *            String email = request.getParameter("email");
     *            String password = request.getParameter("password");
     *
     *            // 2. Recherche user en base (30 lignes JDBC)
     *            Connection conn = null;
     *            PreparedStatement ps = null;
     *            ResultSet rs = null;
     *            User user = null;
     *
     *            try {
     *                conn = dataSource.getConnection();
     *                ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
     *                ps.setString(1, email);
     *                rs = ps.executeQuery();
     *
     *                if (!rs.next()) {
     *                    response.sendError(401, "Identifiants invalides");
     *                    return;
     *                }
     *
     *                // Mapping manuel (15 lignes)
     *                user = new User();
     *                user.setId(rs.getString("id"));
     *                user.setEmail(rs.getString("email"));
     *                user.setPassword(rs.getString("password"));
     *                user.setFailedAttempts(rs.getInt("failed_attempts"));
     *                // ... 10 lignes de mapping
     *            } finally {
     *                // Fermeture (10 lignes)
     *            }
     *
     *            // 3. Vérification verrouillage (20 lignes)
     *            Timestamp lockedUntil = rs.getTimestamp("locked_until");
     *            if (lockedUntil != null) {
     *                LocalDateTime unlockTime = lockedUntil.toLocalDateTime();
     *                if (LocalDateTime.now().isBefore(unlockTime)) {
     *                    response.sendError(423, "Compte verrouillé jusqu'à " + unlockTime);
     *                    return;
     *                } else {
     *                    // Déverrouillage automatique (UPDATE en base - 20 lignes)
     *                    try {
     *                        ps = conn.prepareStatement(
     *                            "UPDATE users SET status = 'ACTIVE', failed_attempts = 0, " +
     *                            "locked_until = NULL WHERE id = ?");
     *                        ps.setString(1, user.getId());
     *                        ps.executeUpdate();
     *                    } finally { }
     *                }
     *            }
     *
     *            // 4. Vérification mot de passe (5 lignes)
     *            if (!BCrypt.checkpw(password, user.getPassword())) {
     *                // Incrément failed_attempts (30 lignes JDBC)
     *                try {
     *                    int newAttempts = user.getFailedAttempts() + 1;
     *
     *                    if (newAttempts >= 5) {
     *                        // Verrouillage (UPDATE - 25 lignes)
     *                        ps = conn.prepareStatement(
     *                            "UPDATE users SET failed_attempts = ?, status = 'LOCKED', " +
     *                            "locked_until = ? WHERE id = ?");
     *                        ps.setInt(1, newAttempts);
     *                        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().plusMinutes(30)));
     *                        ps.setString(3, user.getId());
     *                    } else {
     *                        // Simple incrément (15 lignes)
     *                        ps = conn.prepareStatement(
     *                            "UPDATE users SET failed_attempts = ? WHERE id = ?");
     *                        ps.setInt(1, newAttempts);
     *                        ps.setString(2, user.getId());
     *                    }
     *                    ps.executeUpdate();
     *                } finally { }
     *
     *                response.sendError(401, "Identifiants invalides");
     *                return;
     *            }
     *
     *            // 5. Réinitialisation des échecs (15 lignes JDBC)
     *            try {
     *                ps = conn.prepareStatement(
     *                    "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE id = ?");
     *                ps.setString(1, user.getId());
     *                ps.executeUpdate();
     *            } finally { }
     *
     *            // 6. Création session (10 lignes)
     *            HttpSession session = request.getSession(true);
     *            session.setAttribute("userId", user.getId());
     *            session.setAttribute("email", user.getEmail());
     *            session.setAttribute("role", user.getRole());
     *
     *            // 7. Réponse JSON (10 lignes)
     *            response.setContentType("application/json");
     *            PrintWriter out = response.getWriter();
     *            out.write("{\"message\": \"Login success\"}");
     *        }
     *    }
     *
     *    Total : ~200 lignes
     *
     * SERVICE SPRING (~35 lignes) :
     * ────────────────────────────
     *    Voir ci-dessous
     *
     *    Réduction : 82% de code en moins
     */
    @Override
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {

        // Recherche de l'utilisateur
        // Spring Data : 1 ligne vs 30 lignes JDBC
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

        // RG-03 : Vérification du verrouillage avant tentative
        // Méthode métier dans l'entité (encapsulation)
        // SANS Spring : 20+ lignes de code dans le servlet
        // AVEC Spring : 1 ligne (logique dans User.isLocked())
        if (user.isLocked()) {
            throw new LockedException("Compte verrouillé jusqu'à : " + user.getLockedUntil());
        }

        // RG-20 : Vérification du statut du compte
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new DisabledException("Votre compte a été suspendu. Contactez l'administrateur.");
        }

        try {
            /**
             * ════════════════════════════════════════════════════════════════════════
             * AUTHENTICATION MANAGER - Délégation à Spring Security
             * ════════════════════════════════════════════════════════════════════════
             *
             * SANS SPRING SECURITY (Vérification manuelle) :
             * ──────────────────────────────────────────────
             *    if (!BCrypt.checkpw(password, user.getPassword())) {
             *        throw new Exception("Mot de passe incorrect");
             *    }
             *
             *    → 1 ligne mais :
             *      • Pas de gestion du statut du compte (disabled, locked)
             *      • Pas de vérification des credentials expired
             *      • Pas de support multi-provider (LDAP, OAuth, etc.)
             *
             * AVEC SPRING SECURITY :
             * ─────────────────────
             *    authenticationManager.authenticate(
             *        new UsernamePasswordAuthenticationToken(email, password)
             *    );
             *
             *    → Spring Security :
             *      1. Appelle UserDetailsService.loadUserByUsername(email)
             *      2. Récupère le UserDetails (avec password, authorities, status)
             *      3. Appelle PasswordEncoder.matches(rawPassword, encodedPassword)
             *      4. Vérifie isEnabled(), isAccountNonLocked(), isCredentialsNonExpired()
             *      5. Lance des exceptions typées selon le problème :
             *         - BadCredentialsException (mot de passe incorrect)
             *         - LockedException (compte verrouillé)
             *         - DisabledException (compte désactivé)
             *         - CredentialsExpiredException (mot de passe expiré)
             *
             *    → AVANTAGES :
             *      ✓ Toutes les vérifications de sécurité en 1 ligne
             *      ✓ Extensible (LDAP, OAuth2, SAML, etc.)
             *      ✓ Exceptions typées et gérées globalement
             */
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Authentification réussie → réinitialisation des échecs
            // Méthode métier encapsulée dans l'entité
            user.resetFailures();
            userRepository.save(user);

        } catch (BadCredentialsException e) {
            // RG-03 : Gestion des échecs de connexion
            // Méthode métier qui incrémente ET verrouille si nécessaire
            // SANS Spring : 30+ lignes SQL
            // AVEC Spring : 1 ligne (logique dans User.incrementFailures())
            user.incrementFailures(maxFailedAttempts, lockDurationMinutes);
            userRepository.save(user);

            if (user.isLocked()) {
                throw new LockedException(
                        "Trop de tentatives échouées. Compte verrouillé pour " + lockDurationMinutes + " minutes."
                );
            }

            int remaining = maxFailedAttempts - user.getFailedAttempts();
            throw new BadCredentialsException(
                    "Email ou mot de passe incorrect. " + remaining + " tentative(s) restante(s)."
            );
        }

        // Génération et persistance du JWT
        String token = generateAndPersistToken(user, ipAddress, userAgent);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .pseudo(user.getPseudo())
                .role(user.getRole())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LOGOUT - Révocation du token JWT (ajout à la blacklist)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS JWT (Session HttpSession) :
     * ────────────────────────────────
     *    @WebServlet("/logout")
     *    public class LogoutServlet extends HttpServlet {
     *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
     *            HttpSession session = request.getSession(false);
     *            if (session != null) {
     *                session.invalidate(); // Suppression côté serveur
     *            }
     *            response.setStatus(200);
     *        }
     *    }
     *
     *    → Simple car la session est STATEFUL (stockée sur le serveur)
     *    → Logout instantané
     *
     * AVEC JWT (STATELESS) :
     * ─────────────────────
     *    Problème : Le JWT est valide jusqu'à son expiration (claim "exp").
     *               Comment révoquer un token avant son expiration ?
     *
     *    Solution : Blacklist (Document Session avec isRevoked = true)
     *               Lors de chaque requête, JwtAuthFilter vérifie si le token
     *               est dans la blacklist.
     */
    @Override
    public void logout(String token) {
        sessionRepository.findByJwtToken(token)
                .ifPresent(session -> {
                    session.revoke(); // Méthode métier
                    sessionRepository.save(session);
                });
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * FORGOT PASSWORD - Génération du token de réinitialisation
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SERVLET CLASSIQUE (~80 lignes) :
     * ────────────────────────────────
     *    Voir les commentaires dans PasswordResetToken.java
     *
     * SERVICE SPRING (~10 lignes) :
     * ────────────────────────────
     */
    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Suppression des anciens tokens
            resetTokenRepository.deleteByUserId(user.getId());

            // Génération du nouveau token
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

            resetTokenRepository.save(token);

            // Envoi de l'email (simulé)
            System.out.println("[DEV] Token de reset : " + token.getToken());
        });
        // Pas d'exception → toujours 200, même si email inconnu (sécurité)
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * RESET PASSWORD - Réinitialisation du mot de passe avec token
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SERVLET CLASSIQUE (~70 lignes) :
     * ────────────────────────────────
     *    Voir les commentaires dans PasswordResetToken.java
     *
     * SERVICE SPRING (~15 lignes) :
     * ────────────────────────────
     */
    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou expiré."));

        // Vérification de la validité (méthode métier)
        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Token expiré.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));

        // Mise à jour du mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidation du token (méthode métier)
        resetToken.invalidate();
        resetTokenRepository.save(resetToken);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTHODE PRIVÉE UTILITAIRE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Génère un JWT et persiste la session en base pour la blacklist.
     *
     * SANS Spring : 40+ lignes (génération JWT manuelle + INSERT JDBC)
     * AVEC Spring : 15 lignes (JwtUtil + Spring Data)
     */
    private String generateAndPersistToken(User user, String ipAddress, String userAgent) {
        // Génération du JWT avec claims personnalisés
        String token = jwtUtil.generateToken(
                user.getEmail(),
                Map.of(
                        "role", user.getRole().name(),
                        "pseudo", user.getPseudo(),
                        "userId", user.getId()
                )
        );

        // Persistance de la session pour la blacklist
        Session session = Session.builder()
                .userId(user.getId())
                .jwtToken(token)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtExpirationMs / 1000))
                .isRevoked(false)
                .build();

        sessionRepository.save(session);
        return token;
    }
}