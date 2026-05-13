package ma.emsi.game_platform_backend.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  REGISTER REQUEST - DTO AVEC VALIDATION JAKARTA BEAN VALIDATION
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * RÔLE : Data Transfer Object (DTO) pour la requête d'inscription.
 *        Encapsule les données envoyées par le client au format JSON.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : Validation manuelle dans le Servlet                   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * SANS DTO NI VALIDATION AUTOMATIQUE :
 * ────────────────────────────────────
 *    @WebServlet("/register")
 *    public class RegisterServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            // Récupération des paramètres
 *            String email = request.getParameter("email");
 *            String password = request.getParameter("password");
 *            String pseudo = request.getParameter("pseudo");
 *
 *            // VALIDATION MANUELLE (40+ lignes)
 *            // ───────────────────────────────
 *
 *            // Validation email
 *            if (email == null || email.trim().isEmpty()) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Email requis\"}");
 *                return;
 *            }
 *
 *            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
 *            if (!Pattern.matches(emailRegex, email)) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Format email invalide\"}");
 *                return;
 *            }
 *
 *            // Validation password
 *            if (password == null || password.length() < 8) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Mot de passe trop court (min 8 caractères)\"}");
 *                return;
 *            }
 *
 *            if (password.length() > 100) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Mot de passe trop long (max 100 caractères)\"}");
 *                return;
 *            }
 *
 *            // Validation complexité password
 *            String passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$";
 *            if (!Pattern.matches(passwordRegex, password)) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Le mot de passe doit contenir : " +
 *                                         "1 majuscule, 1 chiffre, 1 caractère spécial\"}");
 *                return;
 *            }
 *
 *            // Validation pseudo
 *            if (pseudo == null || pseudo.trim().isEmpty()) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Pseudo requis\"}");
 *                return;
 *            }
 *
 *            if (pseudo.length() < 3) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Pseudo trop court (min 3 caractères)\"}");
 *                return;
 *            }
 *
 *            if (pseudo.length() > 30) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Pseudo trop long (max 30 caractères)\"}");
 *                return;
 *            }
 *
 *            String pseudoRegex = "^[a-zA-Z0-9_-]+$";
 *            if (!Pattern.matches(pseudoRegex, pseudo)) {
 *                response.setStatus(400);
 *                response.getWriter().write("{\"error\": \"Pseudo invalide (lettres, chiffres, _ et - uniquement)\"}");
 *                return;
 *            }
 *
 *            // Logique métier après validation...
 *        }
 *    }
 *
 *    → PROBLÈMES :
 *      • 40+ lignes de validation répétitives
 *      • Code dupliqué si plusieurs servlets gèrent l'inscription
 *      • Difficile à maintenir (modifier une règle = modifier 5 endroits)
 *      • Pas de réutilisation possible
 *      • Messages d'erreur hardcodés en français (pas d'i18n)
 *      • Tests difficiles (dépendance à HttpServletRequest/Response)
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING : DTO + JAKARTA BEAN VALIDATION (JSR-380)                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * AVANTAGES :
 * ──────────
 *
 * ✅ VALIDATION DÉCLARATIVE :
 *    • Annotations sur les champs (lisible et clair)
 *    • Pas de code impératif (if/else répétitifs)
 *    • Règles autodocumentées
 *
 * ✅ RÉUTILISABILITÉ :
 *    • Le DTO peut être utilisé dans plusieurs contrôleurs
 *    • Validation appliquée automatiquement partout
 *
 * ✅ TESTABILITÉ :
 *    • Tests unitaires sans dépendance HTTP
 *    • Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
 *    • Set<ConstraintViolation> violations = validator.validate(dto);
 *
 * ✅ MAINTENABILITÉ :
 *    • Modifier une règle = 1 seule annotation à changer
 *    • Ajouter un champ = ajouter l'annotation directement
 *
 * ✅ INTÉGRATION SPRING MVC :
 *    • @Valid dans le contrôleur active automatiquement la validation
 *    • Si invalide → MethodArgumentNotValidException
 *    • Gérée par GlobalExceptionHandler → 400 Bad Request avec détails
 *
 * ✅ INTERNATIONALISATION (i18n) :
 *    • Messages configurables dans messages.properties
 *    • Support multilingue automatique
 *
 * COMPARAISON CHIFFRÉE :
 * ─────────────────────
 *    SERVLET MANUEL : 40+ lignes de validation
 *    SPRING + DTO   : 3 annotations par champ (0 ligne de code)
 *
 *    Réduction : 100% du code de validation éliminé
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @Data : Annotation Lombok qui génère :
 *        • Getters pour tous les champs
 *        • Setters pour tous les champs
 *        • toString()
 *        • equals() et hashCode()
 *
 *        SANS : 30+ lignes de boilerplate
 *        AVEC : 1 annotation
 *
 * @NoArgsConstructor : Génère un constructeur vide (requis pour Jackson)
 * @AllArgsConstructor : Génère un constructeur avec tous les paramètres
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * EMAIL - Validation Jakarta Bean Validation
     * ════════════════════════════════════════════════════════════════════════════
     *
     * ANNOTATIONS DE VALIDATION :
     * ──────────────────────────
     *
     * @NotBlank :
     *    • Valeur non null
     *    • Pas vide après trim() (pas juste des espaces)
     *    • Message par défaut : "ne doit pas être vide"
     *
     *    SANS : if (email == null || email.trim().isEmpty()) { ... }
     *    AVEC : @NotBlank
     *
     * @Email :
     *    • Validation format email avec regex interne
     *    • Conforme à RFC 5322
     *    • Message par défaut : "doit être une adresse électronique bien formée"
     *
     *    SANS : Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)
     *    AVEC : @Email
     *
     * @Size :
     *    • Valide la longueur (min/max)
     *    • Applicable aux String, Collection, Array, Map
     *    • Message par défaut : "la taille doit être entre {min} et {max}"
     *
     *    SANS : if (email.length() > 100) { ... }
     *    AVEC : @Size(max = 100)
     *
     * FLUX DE VALIDATION :
     * ───────────────────
     *
     * 1. Client envoie POST /api/auth/register avec JSON :
     *    { "email": "test", "password": "123", "pseudo": "a" }
     *
     * 2. Spring MVC désérialise JSON → RegisterRequest
     *
     * 3. @Valid dans le contrôleur déclenche la validation :
     *    @PostMapping("/register")
     *    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) { }
     *
     * 4. Spring valide TOUS les champs avec leurs annotations :
     *    • @NotBlank sur email : OK
     *    • @Email sur email : ÉCHEC ("test" n'est pas un email valide)
     *    • @Size(min=8) sur password : ÉCHEC (longueur 3 < 8)
     *    • @Size(min=3) sur pseudo : ÉCHEC (longueur 1 < 3)
     *
     * 5. Si au moins une violation → MethodArgumentNotValidException
     *
     * 6. GlobalExceptionHandler capture l'exception et retourne 400 :
     *    {
     *      "errors": {
     *        "email": "doit être une adresse électronique bien formée",
     *        "password": "la taille doit être entre 8 et 100",
     *        "pseudo": "la taille doit être entre 3 et 30"
     *      }
     *    }
     *
     * 7. Client reçoit 400 Bad Request avec les détails des erreurs
     *
     * MESSAGES PERSONNALISÉS :
     * ───────────────────────
     *
     * Chaque annotation accepte un attribut message :
     *
     * @NotBlank(message = "L'email est obligatoire")
     * @Email(message = "Format d'email invalide")
     * @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères")
     *
     * Ou avec i18n (messages.properties) :
     *
     * @NotBlank(message = "{user.email.required}")
     * @Email(message = "{user.email.invalid}")
     *
     * Fichier messages.properties :
     *    user.email.required=L'email est obligatoire
     *    user.email.invalid=Format d'email invalide
     *
     * Fichier messages_en.properties :
     *    user.email.required=Email is required
     *    user.email.invalid=Invalid email format
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères")
    private String email;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * PASSWORD - Validation avec regex complexe
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @Pattern :
     *    • Validation avec une expression régulière (regex)
     *    • Applicable aux String
     *    • Message par défaut : "doit correspondre à {regexp}"
     *
     * REGEX EXPLIQUÉE :
     * ────────────────
     * ^                    ← Début de la chaîne
     * (?=.*[A-Z])          ← Au moins 1 majuscule (lookahead positif)
     * (?=.*[0-9])          ← Au moins 1 chiffre
     * (?=.*[@#$%^&+=!])    ← Au moins 1 caractère spécial
     * .{8,}                ← Au moins 8 caractères au total
     * $                    ← Fin de la chaîne
     *
     * EXEMPLES :
     * ─────────
     * "password"       → INVALIDE (pas de majuscule, pas de chiffre, pas de spécial)
     * "Password"       → INVALIDE (pas de chiffre, pas de spécial)
     * "Password1"      → INVALIDE (pas de caractère spécial)
     * "Password1!"     → VALIDE ✓
     * "MyP@ssw0rd"     → VALIDE ✓
     *
     * SANS SPRING :
     * ────────────
     *    String passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$";
     *    if (!Pattern.matches(passwordRegex, password)) {
     *        response.sendError(400, "Le mot de passe doit contenir...");
     *        return;
     *    }
     *
     *    → 5 lignes par champ à valider
     *
     * AVEC SPRING :
     * ────────────
     *    @Pattern(regexp = "...", message = "...")
     *
     *    → 1 annotation
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$",
            message = "Le mot de passe doit contenir au moins : 1 majuscule, 1 chiffre et 1 caractère spécial (@#$%^&+=!)"
    )
    private String password;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * PSEUDO - Validation simple
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @Pattern avec caractères autorisés :
     * ───────────────────────────────────
     * ^[a-zA-Z0-9_-]+$
     *    • ^ et $ : début et fin de chaîne
     *    • [a-zA-Z0-9_-] : lettres, chiffres, underscore, tiret
     *    • + : au moins 1 caractère
     *
     * EXEMPLES :
     * ─────────
     * "bilal"          → VALIDE ✓
     * "bilal_123"      → VALIDE ✓
     * "bilal-khouna"   → VALIDE ✓
     * "bilal khouna"   → INVALIDE (espace non autorisé)
     * "bilal@emsi"     → INVALIDE (@ non autorisé)
     */
    @NotBlank(message = "Le pseudo est obligatoire")
    @Size(min = 3, max = 30, message = "Le pseudo doit contenir entre 3 et 30 caractères")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "Le pseudo ne doit contenir que des lettres, chiffres, _ et -"
    )
    private String pseudo;
}