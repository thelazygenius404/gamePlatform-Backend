package ma.emsi.game_platform_backend.iam.model;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.AccountStatus;
import ma.emsi.game_platform_backend.shared.enums.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  ENTITÉ USER - COMPARAISON SPRING vs APPROCHE CLASSIQUE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SANS SPRING (POJO classique + JDBC/MongoDB natif)                 │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. CRÉATION MANUELLE DE LA CLASSE (200+ lignes de boilerplate) :
 *    ──────────────────────────────────────────────────────────────
 *    public class User {
 *        private String id;
 *        private String email;
 *        private String password;
 *        private String pseudo;
 *        private String role;
 *        private String status;
 *        private int failedAttempts;
 *        private LocalDateTime lockedUntil;
 *        private int points;
 *        private int level;
 *        private LocalDateTime createdAt;
 *        private LocalDateTime updatedAt;
 *
 *        // Constructeur vide OBLIGATOIRE pour la réflexion JDBC/JPA
 *        public User() {}
 *
 *        // Constructeur avec tous les paramètres (12 paramètres)
 *        public User(String id, String email, String password, String pseudo,
 *                    String role, String status, int failedAttempts,
 *                    LocalDateTime lockedUntil, int points, int level,
 *                    LocalDateTime createdAt, LocalDateTime updatedAt) {
 *            this.id = id;
 *            this.email = email;
 *            this.password = password;
 *            this.pseudo = pseudo;
 *            this.role = role;
 *            this.status = status;
 *            this.failedAttempts = failedAttempts;
 *            this.lockedUntil = lockedUntil;
 *            this.points = points;
 *            this.level = level;
 *            this.createdAt = createdAt;
 *            this.updatedAt = updatedAt;
 *        }
 *
 *        // Getters (12 méthodes)
 *        public String getId() { return id; }
 *        public String getEmail() { return email; }
 *        public String getPassword() { return password; }
 *        public String getPseudo() { return pseudo; }
 *        public String getRole() { return role; }
 *        public String getStatus() { return status; }
 *        public int getFailedAttempts() { return failedAttempts; }
 *        public LocalDateTime getLockedUntil() { return lockedUntil; }
 *        public int getPoints() { return points; }
 *        public int getLevel() { return level; }
 *        public LocalDateTime getCreatedAt() { return createdAt; }
 *        public LocalDateTime getUpdatedAt() { return updatedAt; }
 *
 *        // Setters (12 méthodes)
 *        public void setId(String id) { this.id = id; }
 *        public void setEmail(String email) { this.email = email; }
 *        public void setPassword(String password) { this.password = password; }
 *        public void setPseudo(String pseudo) { this.pseudo = pseudo; }
 *        public void setRole(String role) { this.role = role; }
 *        public void setStatus(String status) { this.status = status; }
 *        public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
 *        public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
 *        public void setPoints(int points) { this.points = points; }
 *        public void setLevel(int level) { this.level = level; }
 *        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
 *        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
 *
 *        // equals() manuel (20+ lignes)
 *        @Override
 *        public boolean equals(Object o) {
 *            if (this == o) return true;
 *            if (o == null || getClass() != o.getClass()) return false;
 *            User user = (User) o;
 *            return failedAttempts == user.failedAttempts &&
 *                   points == user.points &&
 *                   level == user.level &&
 *                   Objects.equals(id, user.id) &&
 *                   Objects.equals(email, user.email) &&
 *                   Objects.equals(password, user.password) &&
 *                   Objects.equals(pseudo, user.pseudo) &&
 *                   Objects.equals(role, user.role) &&
 *                   Objects.equals(status, user.status) &&
 *                   Objects.equals(lockedUntil, user.lockedUntil) &&
 *                   Objects.equals(createdAt, user.createdAt) &&
 *                   Objects.equals(updatedAt, user.updatedAt);
 *        }
 *
 *        // hashCode() manuel
 *        @Override
 *        public int hashCode() {
 *            return Objects.hash(id, email, password, pseudo, role, status,
 *                              failedAttempts, lockedUntil, points, level,
 *                              createdAt, updatedAt);
 *        }
 *
 *        // toString() manuel
 *        @Override
 *        public String toString() {
 *            return "User{" +
 *                   "id='" + id + '\'' +
 *                   ", email='" + email + '\'' +
 *                   ", pseudo='" + pseudo + '\'' +
 *                   ", role='" + role + '\'' +
 *                   ", status='" + status + '\'' +
 *                   ", failedAttempts=" + failedAttempts +
 *                   ", points=" + points +
 *                   ", level=" + level +
 *                   '}';
 *        }
 *    }
 *
 *    → RÉSULTAT : ~250 lignes de code répétitif (boilerplate)
 *    → MAINTENANCE : Ajouter un champ = modifier 6 endroits (champ, constructeur,
 *                    getter, setter, equals, hashCode, toString)
 *    → ERREURS : Oubli d'un champ dans equals() = bugs subtils difficiles à détecter
 *
 * 2. MAPPING AVEC MONGODB (Code supplémentaire dans le DAO) :
 *    ─────────────────────────────────────────────────────────
 *    // Conversion User → Document MongoDB (à écrire dans chaque méthode save/update)
 *    public Document userToDocument(User user) {
 *        Document doc = new Document();
 *        doc.append("_id", user.getId() != null ? new ObjectId(user.getId()) : new ObjectId());
 *        doc.append("email", user.getEmail());
 *        doc.append("password", user.getPassword());
 *        doc.append("pseudo", user.getPseudo());
 *        doc.append("role", user.getRole());
 *        doc.append("status", user.getStatus());
 *        doc.append("failedAttempts", user.getFailedAttempts());
 *        doc.append("lockedUntil", user.getLockedUntil());
 *        doc.append("points", user.getPoints());
 *        doc.append("level", user.getLevel());
 *        doc.append("createdAt", user.getCreatedAt());
 *        doc.append("updatedAt", user.getUpdatedAt());
 *        return doc;
 *    }
 *
 *    // Conversion Document → User (à écrire dans chaque méthode find)
 *    public User documentToUser(Document doc) {
 *        User user = new User();
 *        user.setId(doc.getObjectId("_id").toString());
 *        user.setEmail(doc.getString("email"));
 *        user.setPassword(doc.getString("password"));
 *        user.setPseudo(doc.getString("pseudo"));
 *        user.setRole(doc.getString("role"));
 *        user.setStatus(doc.getString("status"));
 *        user.setFailedAttempts(doc.getInteger("failedAttempts", 0));
 *        user.setLockedUntil((LocalDateTime) doc.get("lockedUntil"));
 *        user.setPoints(doc.getInteger("points", 0));
 *        user.setLevel(doc.getInteger("level", 1));
 *        user.setCreatedAt((LocalDateTime) doc.get("createdAt"));
 *        user.setUpdatedAt((LocalDateTime) doc.get("updatedAt"));
 *        return user;
 *    }
 *
 *    → 25+ lignes de mapping à dupliquer dans CHAQUE méthode du DAO
 *    → Risque d'erreur : typo dans le nom du champ, mauvais type
 *    → Code fragile : renommer un champ = modifier 3 endroits minimum
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE AVEC SPRING BOOT + LOMBOK + SPRING DATA MONGODB                   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. ANNOTATIONS LOMBOK (Code généré automatiquement à la compilation) :
 *    ────────────────────────────────────────────────────────────────────
 *    @Getter                → Génère tous les getters (12 méthodes)
 *    @Setter                → Génère tous les setters (12 méthodes)
 *    @NoArgsConstructor     → Génère constructeur vide public
 *    @AllArgsConstructor    → Génère constructeur avec tous les paramètres
 *    @Builder               → Génère le pattern Builder pour construction fluide
 *
 *    Exemple d'utilisation du Builder :
 *    ──────────────────────────────────
 *    User user = User.builder()
 *        .email("test@emsi.ma")
 *        .password("hashedPassword")
 *        .pseudo("bilal")
 *        .role(Role.USER)
 *        .status(AccountStatus.ACTIVE)
 *        .failedAttempts(0)
 *        .points(0)
 *        .level(1)
 *        .createdAt(LocalDateTime.now())
 *        .build();
 *
 *    → Le code est généré à la COMPILATION (pas de reflection runtime)
 *    → Visible dans target/classes après compilation
 *    → Performance identique au code écrit à la main
 *    → RÉSULTAT : 250 lignes → 50 lignes (80% de réduction)
 *
 * 2. ANNOTATIONS SPRING DATA MONGODB (Mapping automatique) :
 *    ────────────────────────────────────────────────────────
 *    @Document(collection = "users")
 *        → Indique à Spring Data que cette classe est une entité MongoDB
 *        → Nom de la collection MongoDB : "users"
 *        → Active la sérialisation/désérialisation automatique
 *        → SANS : Il faudrait écrire userToDocument() et documentToUser()
 *                 dans CHAQUE classe DAO (25+ lignes × nombre d'entités)
 *
 *    @Id
 *        → Marque le champ comme clé primaire (_id dans MongoDB)
 *        → Spring Data génère automatiquement un ObjectId si null lors du save()
 *        → SANS : Génération manuelle : user.setId(new ObjectId().toString());
 *
 *    @Indexed(unique = true)
 *        → Crée un index unique sur le champ au démarrage de l'application
 *        → Garantit l'unicité au niveau base de données (intégrité référentielle)
 *        → SANS : Création manuelle via shell MongoDB ou MongoCollection.createIndex()
 *                 Risque d'oubli ou d'incohérence entre environnements
 *
 * 3. @BUILDER.DEFAULT (Valeurs par défaut avec le Builder) :
 *    ────────────────────────────────────────────────────────
 *    @Builder.Default private int failedAttempts = 0;
 *        → Valeur par défaut utilisée automatiquement si non spécifiée
 *        → SANS : User.builder().build() → failedAttempts serait 0 (type primitif)
 *        → AVEC : User.builder().build() → failedAttempts = 0 explicite
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ AVANTAGES DE L'APPROCHE SPRING + LOMBOK                                    │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ✅ RÉDUCTION MASSIVE DU CODE :
 *    • Approche classique : ~250 lignes par entité
 *    • Approche Spring+Lombok : ~50 lignes
 *    • Gain : 80% de code en moins
 *
 * ✅ MAINTENANCE SIMPLIFIÉE :
 *    • Ajouter un champ :
 *      - Classique : modifier 6-7 endroits (constructeur, getters, setters, equals, hashCode, toString, mapping)
 *      - Spring : ajouter 1 ligne de champ → tout le reste généré automatiquement
 *    • Renommer un champ :
 *      - Classique : recherche/remplacement manuel dans 6-7 endroits + risque d'oubli
 *      - Spring : refactoring IDE (Alt+Shift+R) → tout est mis à jour automatiquement
 *
 * ✅ MOINS D'ERREURS :
 *    • equals()/hashCode() générés correctement avec TOUS les champs
 *    • toString() toujours à jour
 *    • Mapping MongoDB sans erreur de typo
 *
 * ✅ LISIBILITÉ :
 *    • On voit uniquement la structure de données et la logique métier
 *    • Pas de pollution visuelle avec du code technique répétitif
 *
 * ✅ PERFORMANCE :
 *    • Lombok génère le bytecode à la compilation (pas de reflection)
 *    • Spring Data cache les metadata des entités au démarrage
 *    • Performance équivalente au code manuel
 *
 * ════════════════════════════════════════════════════════════════════════════════
 */
@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * EMAIL - RÈGLE MÉTIER RG-01 : Unicité de l'email
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING DATA :
     * ─────────────────
     *    MongoDB Shell : db.users.createIndex({ email: 1 }, { unique: true })
     *
     *    OU via Java Driver :
     *    MongoCollection<Document> users = db.getCollection("users");
     *    IndexOptions options = new IndexOptions().unique(true);
     *    users.createIndex(Indexes.ascending("email"), options);
     *
     *    + Vérification manuelle AVANT insertion :
     *    Document existing = users.find(Filters.eq("email", email)).first();
     *    if (existing != null) {
     *        throw new IllegalArgumentException("Email déjà utilisé");
     *    }
     *
     * AVEC SPRING DATA :
     * ─────────────────
     *    @Indexed(unique = true)
     *    → Index créé automatiquement au démarrage de l'application
     *    → MongoDB lance MongoWriteException si violation
     *    → Spring traduit en DuplicateKeyException
     *    → Gérée globalement par @ExceptionHandler
     *
     * AVANTAGES :
     *    ✓ Pas de script SQL/MongoDB à exécuter manuellement
     *    ✓ Index créé de manière déclarative (visible dans le code)
     *    ✓ Cohérence garantie entre tous les environnements
     *    ✓ Gestion d'erreur unifiée
     */
    @Indexed(unique = true)
    private String email;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * PASSWORD - RÈGLE MÉTIER RG-02 : Hachage BCrypt
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING SECURITY :
     * ─────────────────────
     *    // Import manuel de BCrypt
     *    import org.mindrot.jbcrypt.BCrypt;
     *
     *    // Hachage lors de l'inscription
     *    public void register(String email, String rawPassword) {
     *        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
     *        user.setPassword(hashedPassword);
     *        userDAO.save(user);
     *    }
     *
     *    // Vérification lors du login
     *    public boolean login(String email, String rawPassword) {
     *        User user = userDAO.findByEmail(email);
     *        if (user == null) return false;
     *        return BCrypt.checkpw(rawPassword, user.getPassword());
     *    }
     *
     *    → PROBLÈMES :
     *      • Code de hachage dupliqué (register, reset password, change password)
     *      • Gestion manuelle du nombre de rounds (12)
     *      • Risque d'oubli du hachage = faille de sécurité critique
     *
     * AVEC SPRING SECURITY :
     * ─────────────────────
     *    // Bean déclaré dans SecurityConfig
     *    @Bean
     *    public PasswordEncoder passwordEncoder() {
     *        return new BCryptPasswordEncoder(12);
     *    }
     *
     *    // Injection dans les services
     *    @Service
     *    public class AuthServiceImpl {
     *        private final PasswordEncoder passwordEncoder;
     *
     *        public void register(RegisterRequest request) {
     *            String hashed = passwordEncoder.encode(request.getPassword());
     *            user.setPassword(hashed);
     *        }
     *
     *        public void login(LoginRequest request) {
     *            // Spring Security appelle automatiquement passwordEncoder.matches()
     *            authenticationManager.authenticate(
     *                new UsernamePasswordAuthenticationToken(email, password)
     *            );
     *        }
     *    }
     *
     * AVANTAGES :
     *    ✓ Configuration centralisée du nombre de rounds
     *    ✓ Interface PasswordEncoder → facile de changer d'algo (Argon2, PBKDF2)
     *    ✓ Intégration automatique avec AuthenticationManager
     *    ✓ Aucune duplication de code
     */
    private String password;

    @Indexed(unique = true)
    private String pseudo;

    private String avatarUrl;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * ROLE - Gestion des autorisations
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS SPRING SECURITY :
     * ─────────────────────
     *    // Stockage du rôle en session
     *    HttpSession session = request.getSession();
     *    session.setAttribute("role", "ADMIN");
     *
     *    // Vérification manuelle dans CHAQUE Servlet
     *    @WebServlet("/api/admin/*")
     *    public class AdminServlet extends HttpServlet {
     *        protected void doGet(HttpServletRequest req, HttpServletResponse res) {
     *            HttpSession session = req.getSession(false);
     *            if (session == null) {
     *                res.sendError(401, "Non authentifié");
     *                return;
     *            }
     *
     *            String role = (String) session.getAttribute("role");
     *            if (!"ADMIN".equals(role)) {
     *                res.sendError(403, "Accès interdit");
     *                return;
     *            }
     *
     *            // Logique métier...
     *        }
     *    }
     *
     *    → PROBLÈMES :
     *      • Code de vérification dupliqué dans CHAQUE servlet
     *      • Risque d'oubli = faille de sécurité
     *      • Gestion des rôles hiérarchiques complexe
     *      • Tests difficiles
     *
     * AVEC SPRING SECURITY :
     * ─────────────────────
     *    // Configuration déclarative dans SecurityConfig
     *    http.authorizeHttpRequests(auth -> auth
     *        .requestMatchers("/api/admin/**").hasRole("ADMIN")
     *        .requestMatchers("/api/games/premium/**").hasAnyRole("PREMIUM", "ADMIN")
     *    );
     *
     *    // Ou annotation dans les contrôleurs
     *    @PreAuthorize("hasRole('ADMIN')")
     *    @DeleteMapping("/api/games/{id}")
     *    public void deleteGame(@PathVariable String id) { ... }
     *
     * AVANTAGES :
     *    ✓ Configuration centralisée
     *    ✓ Aucune vérification manuelle
     *    ✓ Support des rôles hiérarchiques
     *    ✓ Testable avec @WithMockUser
     */
    private Role role;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * STATUS - Gestion du statut du compte
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Valeurs : ACTIVE, SUSPENDED, LOCKED, DELETED
     *
     * SANS SPRING SECURITY :
     * ─────────────────────
     *    // Vérification manuelle dans le Servlet de login
     *    User user = userDAO.findByEmail(email);
     *    if ("SUSPENDED".equals(user.getStatus())) {
     *        response.sendError(403, "Compte suspendu");
     *        return;
     *    }
     *    if ("LOCKED".equals(user.getStatus())) {
     *        response.sendError(423, "Compte verrouillé");
     *        return;
     *    }
     *
     * AVEC SPRING SECURITY :
     * ─────────────────────
     *    // Dans UserDetailsServiceImpl
     *    UserDetails userDetails = User.builder()
     *        .username(user.getEmail())
     *        .password(user.getPassword())
     *        .disabled(user.getStatus() == AccountStatus.SUSPENDED)
     *        .accountLocked(user.isLocked())
     *        .build();
     *
     *    // Spring Security vérifie automatiquement :
     *    // - isEnabled() → DisabledException si false
     *    // - isAccountNonLocked() → LockedException si false
     *
     * AVANTAGES :
     *    ✓ Vérifications automatiques
     *    ✓ Exceptions typées
     *    ✓ Gestion unifiée via @ExceptionHandler
     */
    private AccountStatus status;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * FAILED_ATTEMPTS - RÈGLE MÉTIER RG-03 : Protection brute force
     * ════════════════════════════════════════════════════════════════════════════
     *
     * RG-03 : Après 5 échecs, le compte est verrouillé 30 minutes.
     *
     * SANS SPRING :
     * ────────────
     *    @WebServlet("/login")
     *    public class LoginServlet extends HttpServlet {
     *        protected void doPost(HttpServletRequest req, HttpServletResponse res) {
     *            User user = userDAO.findByEmail(email);
     *
     *            // Vérification verrouillage
     *            if (user.getLockedUntil() != null &&
     *                LocalDateTime.now().isBefore(user.getLockedUntil())) {
     *                res.sendError(423, "Compte verrouillé");
     *                return;
     *            }
     *
     *            // Vérification mot de passe
     *            if (!BCrypt.checkpw(password, user.getPassword())) {
     *                user.setFailedAttempts(user.getFailedAttempts() + 1);
     *
     *                if (user.getFailedAttempts() >= 5) {
     *                    user.setStatus("LOCKED");
     *                    user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
     *                }
     *
     *                userDAO.update(user);
     *                res.sendError(401, "Identifiants invalides");
     *                return;
     *            }
     *
     *            // Succès
     *            user.setFailedAttempts(0);
     *            userDAO.update(user);
     *        }
     *    }
     *
     *    → PROBLÈMES :
     *      • Logique dispersée dans le servlet
     *      • Code dupliqué si plusieurs points d'authentification
     *      • Difficile à tester unitairement
     *
     * AVEC SPRING SECURITY :
     * ─────────────────────
     *    @Service
     *    public class AuthServiceImpl {
     *        public AuthResponse login(LoginRequest request) {
     *            User user = userRepository.findByEmail(request.getEmail())
     *                .orElseThrow(() -> new BadCredentialsException("..."));
     *
     *            if (user.isLocked()) {
     *                throw new LockedException("Compte verrouillé");
     *            }
     *
     *            try {
     *                authenticationManager.authenticate(...);
     *                user.resetFailures();
     *                userRepository.save(user);
     *            } catch (BadCredentialsException e) {
     *                user.incrementFailures(maxAttempts, lockMinutes);
     *                userRepository.save(user);
     *                throw e;
     *            }
     *        }
     *    }
     *
     * AVANTAGES :
     *    ✓ Logique métier dans le domaine (isLocked(), incrementFailures())
     *    ✓ Service testable unitairement
     *    ✓ Configuration externalisée (application.yml)
     */
    @Builder.Default
    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;

    @Builder.Default
    private int points = 0;

    @Builder.Default
    private int level = 1;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTHODES MÉTIER (Domain-Driven Design - Rich Domain Model)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // PATTERN : Logique métier encapsulée dans l'entité (pas dans les services)
    //
    // SANS SPRING (Anemic Domain Model - Anti-pattern) :
    // ──────────────────────────────────────────────────
    //    // Entité = simple structure de données
    //    public class User {
    //        private int failedAttempts;
    //        public int getFailedAttempts() { return failedAttempts; }
    //        public void setFailedAttempts(int n) { this.failedAttempts = n; }
    //    }
    //
    //    // Logique métier dans le service
    //    public class UserService {
    //        public void incrementFailures(User user) {
    //            user.setFailedAttempts(user.getFailedAttempts() + 1);
    //            if (user.getFailedAttempts() >= 5) {
    //                user.setStatus("LOCKED");
    //                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
    //            }
    //        }
    //    }
    //
    //    → PROBLÈMES :
    //      • Logique métier éloignée des données
    //      • Risque de duplication
    //      • Difficile à maintenir
    //
    // AVEC SPRING (Rich Domain Model - Best Practice) :
    // ─────────────────────────────────────────────────
    //    // Logique métier dans l'entité
    //    public class User {
    //        public void incrementFailures(int maxAttempts, int lockMinutes) {
    //            this.failedAttempts++;
    //            if (this.failedAttempts >= maxAttempts) {
    //                this.status = AccountStatus.LOCKED;
    //                this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
    //            }
    //        }
    //    }
    //
    //    // Service = orchestration
    //    public class AuthService {
    //        user.incrementFailures(maxAttempts, lockMinutes);
    //        userRepository.save(user);
    //    }
    //
    // AVANTAGES :
    //    ✓ Logique métier centralisée et réutilisable
    //    ✓ Code autodocumenté (nom de méthode = intention)
    //    ✓ Tests unitaires sans dépendances
    //    ✓ Respect du principe Tell, Don't Ask
    // ════════════════════════════════════════════════════════════════════════════

    public boolean canAccessPremium() {
        return this.role == Role.PREMIUM || this.role == Role.ADMIN;
    }

    public boolean isLocked() {
        if (this.status == AccountStatus.LOCKED && this.lockedUntil != null) {
            if (LocalDateTime.now().isAfter(this.lockedUntil)) {
                this.status = AccountStatus.ACTIVE;
                this.failedAttempts = 0;
                this.lockedUntil = null;
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE && !isLocked();
    }

    public void incrementFailures(int maxAttempts, int lockMinutes) {
        this.failedAttempts++;
        if (this.failedAttempts >= maxAttempts) {
            this.status = AccountStatus.LOCKED;
            this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void resetFailures() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        if (this.status == AccountStatus.LOCKED) {
            this.status = AccountStatus.ACTIVE;
        }
        this.updatedAt = LocalDateTime.now();
    }
}