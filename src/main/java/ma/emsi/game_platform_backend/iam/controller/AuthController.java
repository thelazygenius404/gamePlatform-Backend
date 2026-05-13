package ma.emsi.game_platform_backend.iam.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.dto.*;
import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.iam.repository.UserRepository;
import ma.emsi.game_platform_backend.iam.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  AUTH CONTROLLER - COMPARAISON @RestController vs HttpServlet
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : HttpServlet (J2EE)                                    │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Dans l'approche J2EE classique, chaque endpoint est un Servlet distinct.
 *
 * 1. ENREGISTREMENT DES SERVLETS (web.xml OU @WebServlet) :
 *    ────────────────────────────────────────────────────────
 *
 *    A. Avec web.xml (configuration XML) :
 *    ─────────────────────────────────────
 *    <?xml version="1.0" encoding="UTF-8"?>
 *    <web-app>
 *        <!-- Servlet Register -->
 *        <servlet>
 *            <servlet-name>RegisterServlet</servlet-name>
 *            <servlet-class>com.emsi.servlets.RegisterServlet</servlet-class>
 *        </servlet>
 *        <servlet-mapping>
 *            <servlet-name>RegisterServlet</servlet-name>
 *            <url-pattern>/api/auth/register</url-pattern>
 *        </servlet-mapping>
 *
 *        <!-- Servlet Login -->
 *        <servlet>
 *            <servlet-name>LoginServlet</servlet-name>
 *            <servlet-class>com.emsi.servlets.LoginServlet</servlet-class>
 *        </servlet>
 *        <servlet-mapping>
 *            <servlet-name>LoginServlet</servlet-name>
 *            <url-pattern>/api/auth/login</url-pattern>
 *        </servlet-mapping>
 *
 *        <!-- Servlet Logout -->
 *        <servlet>
 *            <servlet-name>LogoutServlet</servlet-name>
 *            <servlet-class>com.emsi.servlets.LogoutServlet</servlet-class>
 *        </servlet>
 *        <servlet-mapping>
 *            <servlet-name>LogoutServlet</servlet-name>
 *            <url-pattern>/api/auth/logout</url-pattern>
 *        </servlet-mapping>
 *
 *        <!-- ... 3 servlets supplémentaires pour forgot/reset/me -->
 *    </web-app>
 *
 *    → PROBLÈMES :
 *      • Configuration XML verbeux (10+ lignes par servlet)
 *      • Séparé du code Java (risque d'incohérence)
 *      • Difficile à maintenir (renommer une classe = modifier web.xml)
 *
 *    B. Avec @WebServlet (annotation Java EE 6+) :
 *    ─────────────────────────────────────────────
 *    @WebServlet("/api/auth/register")
 *    public class RegisterServlet extends HttpServlet { }
 *
 *    @WebServlet("/api/auth/login")
 *    public class LoginServlet extends HttpServlet { }
 *
 *    @WebServlet("/api/auth/logout")
 *    public class LogoutServlet extends HttpServlet { }
 *
 *    → Mieux que web.xml mais :
 *      • 1 classe par endpoint = explosion du nombre de fichiers
 *      • Pas de regroupement logique (auth dispersé dans 6 servlets)
 *      • Pas de gestion centralisée des préfixes (/api/auth/)
 *
 * 2. IMPLÉMENTATION D'UN SERVLET (RegisterServlet) :
 *    ─────────────────────────────────────────────────
 *
 *    @WebServlet("/api/auth/register")
 *    public class RegisterServlet extends HttpServlet {
 *
 *        // Injection manuelle des dépendances (problématique)
 *        private UserDAO userDAO;
 *        private SessionDAO sessionDAO;
 *
 *        @Override
 *        public void init() throws ServletException {
 *            // Initialisation manuelle au démarrage du servlet
 *            this.userDAO = new UserDAO();
 *            this.sessionDAO = new SessionDAO();
 *        }
 *
 *        @Override
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *                throws ServletException, IOException {
 *
 *            // ══════════════════════════════════════════════════════════════
 *            // 1. RÉCUPÉRATION DES PARAMÈTRES (15+ lignes)
 *            // ══════════════════════════════════════════════════════════════
 *
 *            // Content-Type : application/json
 *            StringBuilder sb = new StringBuilder();
 *            String line;
 *            try (BufferedReader reader = request.getReader()) {
 *                while ((line = reader.readLine()) != null) {
 *                    sb.append(line);
 *                }
 *            }
 *
 *            // Parsing JSON manuel (avec Jackson ou Gson)
 *            ObjectMapper mapper = new ObjectMapper();
 *            JsonNode json = mapper.readTree(sb.toString());
 *
 *            String email = json.get("email").asText();
 *            String password = json.get("password").asText();
 *            String pseudo = json.get("pseudo").asText();
 *
 *            // OU Content-Type : application/x-www-form-urlencoded
 *            // String email = request.getParameter("email");
 *            // String password = request.getParameter("password");
 *
 *            // ══════════════════════════════════════════════════════════════
 *            // 2. VALIDATION MANUELLE (40+ lignes)
 *            // ══════════════════════════════════════════════════════════════
 *
 *            response.setContentType("application/json");
 *            response.setCharacterEncoding("UTF-8");
 *            PrintWriter out = response.getWriter();
 *
 *            // Validation email
 *            if (email == null || email.isEmpty()) {
 *                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *                out.write("{\"error\": \"L'email est obligatoire\"}");
 *                return;
 *            }
 *
 *            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
 *            Pattern emailPattern = Pattern.compile(emailRegex);
 *            if (!emailPattern.matcher(email).matches()) {
 *                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *                out.write("{\"error\": \"Format d'email invalide\"}");
 *                return;
 *            }
 *
 *            // Validation password
 *            if (password == null || password.length() < 8) {
 *                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *                out.write("{\"error\": \"Le mot de passe doit contenir au moins 8 caractères\"}");
 *                return;
 *            }
 *
 *            String passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$";
 *            Pattern passwordPattern = Pattern.compile(passwordRegex);
 *            if (!passwordPattern.matcher(password).matches()) {
 *                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *                out.write("{\"error\": \"Le mot de passe doit contenir une majuscule, un chiffre et un caractère spécial\"}");
 *                return;
 *            }
 *
 *            // Validation pseudo
 *            if (pseudo == null || pseudo.length() < 3 || pseudo.length() > 30) {
 *                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *                out.write("{\"error\": \"Le pseudo doit avoir entre 3 et 30 caractères\"}");
 *                return;
 *            }
 *
 *            // ══════════════════════════════════════════════════════════════
 *            // 3. LOGIQUE MÉTIER (voir AuthServiceImpl pour détails)
 *            // ══════════════════════════════════════════════════════════════
 *
 *            // Vérification unicité (DAO)
 *            if (userDAO.existsByEmail(email)) {
 *                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *                out.write("{\"error\": \"Cet email est déjà utilisé\"}");
 *                return;
 *            }
 *
 *            // Hachage BCrypt
 *            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
 *
 *            // Création user
 *            User user = new User();
 *            user.setId(UUID.randomUUID().toString());
 *            user.setEmail(email);
 *            user.setPassword(hashedPassword);
 *            user.setPseudo(pseudo);
 *            user.setRole("USER");
 *            user.setStatus("ACTIVE");
 *            user.setFailedAttempts(0);
 *            user.setPoints(0);
 *            user.setLevel(1);
 *            user.setCreatedAt(LocalDateTime.now());
 *            user.setUpdatedAt(LocalDateTime.now());
 *
 *            // Sauvegarde (DAO)
 *            try {
 *                userDAO.save(user);
 *            } catch (Exception e) {
 *                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 *                out.write("{\"error\": \"Erreur lors de l'inscription\"}");
 *                return;
 *            }
 *
 *            // Création session
 *            HttpSession session = request.getSession(true);
 *            session.setAttribute("userId", user.getId());
 *            session.setAttribute("email", user.getEmail());
 *            session.setAttribute("role", user.getRole());
 *
 *            // ══════════════════════════════════════════════════════════════
 *            // 4. RÉPONSE JSON MANUELLE (10+ lignes)
 *            // ══════════════════════════════════════════════════════════════
 *
 *            response.setStatus(HttpServletResponse.SC_CREATED);
 *
 *            // Construction JSON manuelle (sans Jackson : encore plus de code)
 *            ObjectMapper responseMapper = new ObjectMapper();
 *            ObjectNode responseJson = responseMapper.createObjectNode();
 *            responseJson.put("message", "Inscription réussie");
 *            responseJson.put("userId", user.getId());
 *            responseJson.put("email", user.getEmail());
 *            responseJson.put("pseudo", user.getPseudo());
 *
 *            out.write(responseMapper.writeValueAsString(responseJson));
 *        }
 *
 *        // Gestion des autres méthodes HTTP
 *        @Override
 *        protected void doGet(HttpServletRequest request, HttpServletResponse response) {
 *            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
 *        }
 *
 *        @Override
 *        protected void doPut(HttpServletRequest request, HttpServletResponse response) {
 *            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
 *        }
 *
 *        @Override
 *        protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
 *            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
 *        }
 *    }
 *
 *    → BILAN SERVLET :
 *      • ~150-200 lignes par endpoint
 *      • Validation manuelle répétitive
 *      • Parsing JSON manuel
 *      • Sérialisation JSON manuelle
 *      • Gestion manuelle des status HTTP
 *      • Logique métier mélangée avec le code HTTP
 *      • Pas de gestion centralisée des erreurs
 *      • Pas de typage fort (String partout)
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING : @RestController                                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. UN SEUL FICHIER POUR TOUS LES ENDPOINTS AUTH :
 *    ───────────────────────────────────────────────
 *
 *    @RestController
 *    @RequestMapping("/api/auth")
 *    public class AuthController {
 *
 *        @PostMapping("/register")
 *        public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { }
 *
 *        @PostMapping("/login")
 *        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) { }
 *
 *        @PostMapping("/logout")
 *        public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) { }
 *
 *        @PostMapping("/forgot-password")
 *        public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) { }
 *
 *        @PostMapping("/reset-password")
 *        public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) { }
 *
 *        @GetMapping("/me")
 *        public ResponseEntity<Map<String, Object>> getCurrentUser() { }
 *    }
 *
 *    → AVANTAGES :
 *      ✓ 1 seul fichier au lieu de 6 servlets
 *      ✓ Préfixe commun @RequestMapping("/api/auth")
 *      ✓ Organisation logique claire
 *      ✓ ~20 lignes par endpoint vs 150-200
 *
 * 2. ANNOTATIONS SPRING MVC :
 *    ─────────────────────────
 *
 *    @RestController :
 *    ────────────────
 *      = @Controller + @ResponseBody
 *      • Tous les retours sont automatiquement sérialisés en JSON
 *      • Pas besoin de response.getWriter().write(json)
 *
 *    @RequestMapping("/api/auth") :
 *    ─────────────────────────────
 *      • Préfixe commun pour tous les endpoints de ce contrôleur
 *      • SANS : répéter "/api/auth" dans chaque @PostMapping
 *
 *    @PostMapping("/register") :
 *    ──────────────────────────
 *      = @RequestMapping(value = "/register", method = RequestMethod.POST)
 *      • Plus concis et lisible
 *      • Méthode HTTP typée (erreur 405 automatique si GET/PUT/DELETE)
 *
 *    @Valid :
 *    ───────
 *      • Active la validation Jakarta Bean Validation (JSR-380)
 *      • Les contraintes (@NotBlank, @Email, @Pattern) définies dans le DTO
 *        sont vérifiées automatiquement
 *      • Si invalide → MethodArgumentNotValidException → 400 Bad Request
 *      • Géré par @ControllerAdvice (GlobalExceptionHandler)
 *
 *    @RequestBody :
 *    ─────────────
 *      • Désérialisation automatique JSON → Objet Java (via Jackson)
 *      • SANS : lecture manuelle + parsing JSON (15+ lignes)
 *      • AVEC : 1 annotation
 *
 *    ResponseEntity<T> :
 *    ──────────────────
 *      • Contrôle du status HTTP (200, 201, 400, 401, etc.)
 *      • Contrôle des headers HTTP
 *      • Corps de réponse typé (T)
 *      • Sérialisation automatique en JSON
 *
 * 3. COMPARAISON ENDPOINT REGISTER :
 *    ────────────────────────────────
 *
 *    SERVLET (~150 lignes) :
 *    ──────────────────────
 *    • Parsing JSON manuel : 15 lignes
 *    • Validation manuelle : 40 lignes
 *    • Logique métier : 50 lignes
 *    • Sérialisation JSON : 10 lignes
 *    • Gestion erreurs : 30 lignes
 *
 *    SPRING (~10 lignes) :
 *    ────────────────────
 *    @PostMapping("/register")
 *    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
 *        AuthResponse response = authService.register(request);
 *        return ResponseEntity.status(HttpStatus.CREATED).body(response);
 *    }
 *
 *    → Réduction : 93% de code en moins
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @RestController : Combinaison de @Controller et @ResponseBody.
 *                   Indique que tous les retours de méthodes sont sérialisés
 *                   en JSON automatiquement.
 *
 *                   SANS @RestController :
 *                     response.setContentType("application/json");
 *                     response.getWriter().write(mapper.writeValueAsString(object));
 *
 *                   AVEC @RestController :
 *                     return object; // Sérialisation automatique par Jackson
 *
 * @RequestMapping("/api/auth") : Préfixe commun pour tous les endpoints.
 *                                 Équivaut à un "namespace" pour ce contrôleur.
 *
 * @RequiredArgsConstructor : Génère un constructeur avec les dépendances final.
 *                            Spring utilise ce constructeur pour l'injection.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * INJECTION DE DÉPENDANCES
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING (Servlet) :
     * ──────────────────────
     *    public class RegisterServlet extends HttpServlet {
     *        private UserDAO userDAO;
     *
     *        @Override
     *        public void init() {
     *            this.userDAO = new UserDAO(); // Instanciation manuelle
     *        }
     *    }
     *
     *    → Couplage fort, tests difficiles
     *
     * AVEC SPRING :
     * ────────────
     *    @RestController
     *    @RequiredArgsConstructor
     *    public class AuthController {
     *        private final AuthService authService; // Injecté par Spring
     *    }
     *
     *    → Découplage, testable facilement (injection de mocks)
     */
    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * POST /api/auth/register - Inscription
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SERVLET CLASSIQUE (~150 lignes) :
     * ─────────────────────────────────
     *    Voir le bloc de commentaires au début du fichier
     *
     * SPRING (~4 lignes) :
     * ───────────────────
     *    @PostMapping("/register")
     *    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
     *        AuthResponse response = authService.register(request);
     *        return ResponseEntity.status(HttpStatus.CREATED).body(response);
     *    }
     *
     * EXPLICATIONS DES ANNOTATIONS :
     * ──────────────────────────────
     *
     * @PostMapping("/register") :
     *    • URL complète : /api/auth/register (préfixe + suffixe)
     *    • Méthode HTTP : POST uniquement
     *    • GET/PUT/DELETE → 405 Method Not Allowed automatique
     *
     * @Valid :
     *    • Active la validation Jakarta Bean Validation
     *    • Les contraintes dans RegisterRequest sont vérifiées :
     *      - @NotBlank sur email/password/pseudo
     *      - @Email sur email
     *      - @Pattern sur password (majuscule, chiffre, caractère spécial)
     *    • Si validation échoue → MethodArgumentNotValidException
     *    • Gérée par GlobalExceptionHandler → 400 Bad Request avec détails
     *
     * @RequestBody :
     *    • Désérialisation automatique JSON → RegisterRequest
     *    • Jackson lit le Content-Type (application/json)
     *    • Mapping automatique des champs JSON vers les propriétés Java
     *    • SANS : 15 lignes de parsing JSON manuel
     *
     * ResponseEntity<AuthResponse> :
     *    • Permet de contrôler le status HTTP (201 Created ici)
     *    • Body typé (AuthResponse)
     *    • Sérialisation automatique AuthResponse → JSON par Jackson
     *    • SANS : 10 lignes de construction JSON manuelle
     *
     * FLUX COMPLET :
     * ─────────────
     * 1. Client envoie POST /api/auth/register avec JSON :
     *    { "email": "test@emsi.ma", "password": "Pass@123", "pseudo": "bilal" }
     *
     * 2. DispatcherServlet (contrôleur frontal Spring MVC) reçoit la requête
     *
     * 3. Spring MVC trouve le handler : AuthController.register()
     *
     * 4. Spring désérialise JSON → RegisterRequest via Jackson
     *
     * 5. Spring valide RegisterRequest via @Valid (contraintes JSR-380)
     *    Si invalide → MethodArgumentNotValidException → GlobalExceptionHandler → 400
     *
     * 6. Spring appelle authService.register(request)
     *
     * 7. Le service exécute la logique métier et retourne AuthResponse
     *
     * 8. Spring sérialise AuthResponse → JSON via Jackson
     *
     * 9. Client reçoit 201 Created avec JSON :
     *    { "token": "eyJhbG...", "userId": "123", "email": "test@emsi.ma", ... }
     *
     * AVANTAGES :
     * ──────────
     * ✅ 4 lignes vs 150 lignes (96% de réduction)
     * ✅ Validation déclarative (@Valid) vs manuelle (40 lignes)
     * ✅ Parsing automatique vs manuel (15 lignes)
     * ✅ Sérialisation automatique vs manuelle (10 lignes)
     * ✅ Gestion erreurs centralisée vs dispersée
     * ✅ Code lisible et maintenable
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * POST /api/auth/login - Connexion
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SERVLET CLASSIQUE (~200 lignes) :
     * ─────────────────────────────────
     *    Voir AuthServiceImpl.java pour détails sur la logique
     *
     * SPRING (~5 lignes) :
     * ───────────────────
     *
     * HttpServletRequest :
     *    • Injecté automatiquement par Spring MVC
     *    • Permet d'accéder aux informations de la requête HTTP
     *    • Ici : extraction de l'IP et du User-Agent pour audit
     *    • SANS Spring : paramètre de la méthode doPost() du servlet
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * POST /api/auth/logout - Déconnexion
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SERVLET CLASSIQUE (~20 lignes) :
     * ────────────────────────────────
     *    @WebServlet("/api/auth/logout")
     *    public class LogoutServlet extends HttpServlet {
     *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
     *            HttpSession session = request.getSession(false);
     *            if (session != null) {
     *                session.invalidate();
     *            }
     *
     *            response.setStatus(HttpServletResponse.SC_OK);
     *            response.setContentType("application/json");
     *            PrintWriter out = response.getWriter();
     *            out.write("{\"message\": \"Déconnexion réussie\"}");
     *        }
     *    }
     *
     * SPRING (~7 lignes) :
     * ───────────────────
     *
     * Extraction du token JWT depuis le header Authorization :
     *    Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
     *
     *    SANS Spring :
     *      String authHeader = request.getHeader("Authorization");
     *      if (authHeader != null && authHeader.startsWith("Bearer ")) {
     *          String token = authHeader.substring(7);
     *      }
     *
     *    AVEC Spring : Même code (pas d'abstraction spécifique pour ça)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie."));
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * POST /api/auth/forgot-password - Demande de réinitialisation
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Map<String, String> :
     *    • Permet de recevoir un JSON simple sans créer de DTO
     *    • Utile pour les endpoints simples (1-2 champs)
     *    • Jackson désérialise automatiquement { "email": "..." } → Map
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        authService.forgotPassword(body.get("email"));
        // Message générique pour ne pas révéler si l'email existe
        return ResponseEntity.ok(Map.of(
                "message", "Si cet email est associé à un compte, un lien de réinitialisation a été envoyé."
        ));
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * POST /api/auth/reset-password - Réinitialisation du mot de passe
     * ════════════════════════════════════════════════════════════════════════════
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès."));
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * GET /api/auth/me - Récupération des informations de l'utilisateur connecté
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SERVLET CLASSIQUE (~30 lignes) :
     * ────────────────────────────────
     *    @WebServlet("/api/auth/me")
     *    public class MeServlet extends HttpServlet {
     *        protected void doGet(HttpServletRequest request, HttpServletResponse response) {
     *            // Récupération de la session
     *            HttpSession session = request.getSession(false);
     *            if (session == null) {
     *                response.sendError(401, "Non authentifié");
     *                return;
     *            }
     *
     *            String userId = (String) session.getAttribute("userId");
     *            if (userId == null) {
     *                response.sendError(401, "Non authentifié");
     *                return;
     *            }
     *
     *            // Récupération de l'utilisateur en base (DAO)
     *            User user = userDAO.findById(userId);
     *            if (user == null) {
     *                response.sendError(404, "Utilisateur non trouvé");
     *                return;
     *            }
     *
     *            // Construction JSON manuelle
     *            response.setContentType("application/json");
     *            ObjectMapper mapper = new ObjectMapper();
     *            ObjectNode json = mapper.createObjectNode();
     *            json.put("userId", user.getId());
     *            json.put("email", user.getEmail());
     *            json.put("pseudo", user.getPseudo());
     *            json.put("role", user.getRole());
     *            // ... autres champs
     *
     *            response.getWriter().write(mapper.writeValueAsString(json));
     *        }
     *    }
     *
     * SPRING (~10 lignes) :
     * ────────────────────
     *
     * @AuthenticationPrincipal :
     *    • Injecte automatiquement l'utilisateur connecté
     *    • Spring Security extrait le Principal du SecurityContext
     *    • Le Principal est un UserDetails (interface Spring Security)
     *    • SANS : récupération manuelle depuis la session
     *
     *    Flux :
     *    ─────
     *    1. Client envoie GET /api/auth/me avec Authorization: Bearer token
     *    2. JwtAuthFilter intercepte la requête
     *    3. JwtAuthFilter valide le token et place un Authentication dans le SecurityContext
     *    4. Spring MVC appelle cette méthode
     *    5. Spring injecte le UserDetails depuis SecurityContext.getAuthentication().getPrincipal()
     *    6. On récupère le username (email) depuis UserDetails
     *    7. On cherche l'utilisateur en base
     *    8. Spring sérialise la réponse en JSON automatiquement
     *
     * UserDetails :
     *    • Interface Spring Security représentant un utilisateur authentifié
     *    • Contient : username, password, authorities, status (enabled, locked, etc.)
     *    • Implémentation : classe interne User de Spring Security
     *    • Créé dans UserDetailsServiceImpl.loadUserByUsername()
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ResponseEntity.ok(Map.of(
                "userId",  user.getId(),
                "email",   user.getEmail(),
                "pseudo",  user.getPseudo(),
                "role",    user.getRole(),
                "level",   user.getLevel(),
                "points",  user.getPoints(),
                "status",  user.getStatus()
        ));
    }
}