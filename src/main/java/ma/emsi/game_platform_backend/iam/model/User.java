package ma.emsi.game_platform_backend.iam.model;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.AccountStatus;
import ma.emsi.game_platform_backend.shared.enums.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ============================================================
 *  Document MongoDB : User
 * ============================================================
 *
 * ANNOTATION @Document :
 * ----------------------
 * SANS Spring Data MongoDB :
 *   On manipulerait un objet org.bson.Document (ou un Map) directement :
 *     Document doc = new Document("email", user.getEmail())
 *                        .append("password", hashedPwd)
 *                        .append("role", "USER");
 *     collection.insertOne(doc);
 *   → Pas de mapping objet, risque d'erreur de typo sur les noms de champs,
 *     aucune validation automatique, conversion manuelle partout.
 *
 * AVEC Spring Data MongoDB :
 *   @Document(collection = "users") mappe automatiquement la classe Java
 *   vers la collection MongoDB. MongoTemplate et les Repositories gèrent
 *   la sérialisation/désérialisation (POJO ↔ BSON) via MappingMongoConverter.
 *
 * ANNOTATION @Id :
 * ----------------
 * SANS Spring : on gérerait la génération d'identifiants manuellement
 *   (UUID.randomUUID().toString() ou ObjectId de MongoDB).
 * AVEC Spring : @Id sur un champ String génère automatiquement un ObjectId
 *   MongoDB et le mappe au champ "_id".
 *
 * ANNOTATION @Indexed(unique = true) :
 * ------------------------------------
 * Spring Data crée l'index MongoDB automatiquement si auto-index-creation=true.
 * SANS Spring : CREATE INDEX via le shell MongoDB ou MongoCollection.createIndex().
 * ============================================================
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
     * RG-01 : email unique en base.
     * @Indexed(unique=true) → Spring Data crée l'index { email: 1 } unique.
     * SANS Spring : db.users.createIndex({ email: 1 }, { unique: true })
     */
    @Indexed(unique = true)
    private String email;

    /**
     * Mot de passe haché avec BCrypt.
     * SANS Spring : BCrypt.hashpw(rawPassword, BCrypt.gensalt(12)) manuellement.
     * AVEC Spring Security : PasswordEncoder (BCryptPasswordEncoder) injecté,
     * appelé via passwordEncoder.encode(rawPassword) dans le service.
     * Spring Security vérifie avec passwordEncoder.matches(raw, encoded).
     */
    private String password;

    @Indexed(unique = true)
    private String pseudo;

    private String avatarUrl;

    /**
     * SANS Spring Security : on stockerait une String et ferait des comparaisons
     * manuelles. AVEC Spring : Role est utilisé dans UserDetails.getAuthorities()
     * pour construire un SimpleGrantedAuthority("ROLE_" + role.name()).
     */
    private Role role;

    /**
     * Statut du compte — pilote les vérifications dans UserDetails :
     * isEnabled()          → status == ACTIVE
     * isAccountNonLocked() → status != LOCKED
     */
    private AccountStatus status;

    /**
     * RG-03 : Compteur d'échecs de connexion.
     * SANS Spring : incrément manuel en SQL/DAO + comparaison dans le Servlet de login.
     * AVEC Spring : géré dans AuthServiceImpl, réinitialisé après succès,
     * verrouillage automatique si failedAttempts >= maxFailedAttempts.
     */
    @Builder.Default
    private int failedAttempts = 0;

    /**
     * Date/heure de fin du verrouillage temporaire (RG-03 : 30 minutes).
     * null si le compte n'est pas verrouillé temporairement.
     */
    private LocalDateTime lockedUntil;

    /** Points gamification (Phase 3) */
    @Builder.Default
    private int points = 0;

    /** Niveau calculé depuis les points */
    @Builder.Default
    private int level = 1;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ================================================================
    // MÉTHODES MÉTIER (Domain Logic — cohérent avec la conception v2)
    // ================================================================

    /**
     * RG-07 : Vérifier si l'utilisateur peut accéder aux jeux premium.
     * Logique centralisée dans le domaine → pas de duplication dans les contrôleurs.
     */
    public boolean canAccessPremium() {
        return this.role == Role.PREMIUM || this.role == Role.ADMIN;
    }

    /**
     * RG-03 : Vérifier si le compte est actuellement verrouillé.
     * SANS Spring : vérification manuelle dans chaque filtre Servlet.
     * AVEC Spring Security : Spring appelle isAccountNonLocked() automatiquement
     * lors de l'authentification via AuthenticationManager.
     */
    public boolean isLocked() {
        if (this.status == AccountStatus.LOCKED && this.lockedUntil != null) {
            // Si le temps de verrouillage est écoulé → déverrouillage automatique
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

    /**
     * Vérifie si le compte est actif (non suspendu, non supprimé, non verrouillé).
     */
    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE && !isLocked();
    }

    /**
     * Incrémente le compteur d'échecs et verrouille si le seuil est atteint.
     * @param maxAttempts seuil lu depuis application.yml
     * @param lockMinutes durée du verrouillage en minutes
     */
    public void incrementFailures(int maxAttempts, int lockMinutes) {
        this.failedAttempts++;
        if (this.failedAttempts >= maxAttempts) {
            this.status = AccountStatus.LOCKED;
            this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /** Réinitialise les échecs après une connexion réussie. */
    public void resetFailures() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        if (this.status == AccountStatus.LOCKED) {
            this.status = AccountStatus.ACTIVE;
        }
        this.updatedAt = LocalDateTime.now();
    }
}