package ma.emsi.game_platform_backend.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * LOGIN REQUEST - DTO AVEC VALIDATION JAKARTA BEAN VALIDATION
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * RÔLE : Data Transfer Object (DTO) pour la requête de connexion.
 * Encapsule les identifiants (email + mot de passe) envoyés par le client
 * au format JSON lors de l'appel à l'API de login.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : Validation manuelle dans le Servlet                   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * SANS DTO NI VALIDATION AUTOMATIQUE :
 * ────────────────────────────────────
 * @WebServlet("/api/auth/login")
 * public class LoginServlet extends HttpServlet {
 * protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 * // 1. Récupération
 * String email = request.getParameter("email");
 * String password = request.getParameter("password");
 *
 * // 2. VALIDATION MANUELLE (15+ lignes)
 * if (email == null || email.trim().isEmpty()) {
 * response.setStatus(400);
 * response.getWriter().write("{\"error\": \"L'email est requis\"}");
 * return;
 * }
 *
 * if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
 * response.setStatus(400);
 * response.getWriter().write("{\"error\": \"Format d'email invalide\"}");
 * return;
 * }
 *
 * if (password == null || password.trim().isEmpty()) {
 * response.setStatus(400);
 * response.getWriter().write("{\"error\": \"Le mot de passe est requis\"}");
 * return;
 * }
 *
 * // 3. Logique d'authentification (DAO, hachage, etc.)...
 * }
 * }
 *
 * → PROBLÈMES :
 * • Logique de validation mélangée avec le code du contrôleur
 * • Répétitif (on refait le même if(email == null) qu'à l'inscription)
 * • Gestion manuelle des réponses HTTP 400
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING : Validation déclarative (JSR-380)                         │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * AVANTAGES :
 * ──────────
 * ✅ LISIBILITÉ : Les règles de validation sont de simples annotations.
 * ✅ DÉCOUPLAGE : Le contrôleur fait confiance au DTO. S'il atteint la
 * méthode du contrôleur, c'est que les données sont valides.
 * ✅ GESTION GLOBALE : En cas d'erreur, Spring lève une exception qui est
 * interceptée globalement pour renvoyer une erreur 400 propre.
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ANNOTATIONS LOMBOK :
 * ───────────────────
 * @Getter / @Setter : Génèrent automatiquement getEmail(), getPassword(), etc.
 * @NoArgsConstructor : Requis par Spring (Jackson) pour désérialiser le JSON
 * entrant en objet Java via réflexion.
 * @AllArgsConstructor : Utile pour instancier rapidement l'objet (ex: dans les tests).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * EMAIL
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @NotBlank :
     * Vérifie que l'email n'est pas null, pas vide, et ne contient pas
     * que des espaces.
     * Remplace : if(email == null || email.trim().isEmpty())
     *
     * @Email :
     * Vérifie que la chaîne a bien un format d'email valide.
     * Remplace : la vérification par Regex manuelle.
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    private String email;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * PASSWORD
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @NotBlank :
     * Contrairement au RegisterRequest, ici on ne vérifie pas la complexité
     * (@Pattern) ou la taille (@Size) du mot de passe.
     *
     * POURQUOI ?
     * Parce que lors de la connexion, notre seul but est de vérifier si le
     * client a fourni "un" mot de passe. C'est l'AuthenticationManager (et la BDD)
     * qui se chargeront de dire si ce mot de passe est correct ou non.
     * Refaire une validation regex ici est inutile et gaspille des ressources.
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}