package ma.emsi.game_platform_backend.iam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Document MongoDB représentant une session JWT active (liste noire / révocation).
 *
 * ============================================================
 * APPROCHE CLASSIQUE (sans Spring / sans JWT) :
 * ============================================================
 * En J2EE classique, la session est gérée par le serveur via HttpSession :
 *
 *   HttpSession session = request.getSession(true);
 *   session.setAttribute("userId", user.getId());
 *   session.setAttribute("role", user.getRole());
 *   // Le serveur stocke l'état en mémoire (ou via un SessionManager)
 *   // Le client reçoit un cookie JSESSIONID
 *
 * Inconvénients :
 *  - Stateful : le serveur doit stocker l'état de chaque session
 *  - Scalabilité : problème avec les clusters (sticky sessions ou session partagée)
 *  - Invalidation : session.invalidate() côté serveur uniquement
 *
 * ============================================================
 * APPROCHE JWT (stateless) :
 * ============================================================
 * Le token JWT contient les claims (userId, role, exp) signés avec une clé secrète.
 * Le serveur ne stocke RIEN en mémoire pour valider — il vérifie la signature.
 * Ce document sert uniquement pour la RÉVOCATION (blacklist) des tokens :
 * si un token est révoqué (logout), on l'enregistre ici pour le bloquer même
 * si la signature est valide.
 *
 * Avantages JWT :
 *  - Stateless et scalable horizontalement
 *  - Pas de lookup DB à chaque requête (sauf révocation)
 *  - Portable (API mobile + web avec le même mécanisme)
 * ============================================================
 *
 * TTL INDEX : @Indexed(expireAfterSeconds) → MongoDB supprime automatiquement
 * les documents expirés. SANS Spring : il faudrait un job cron manuel.
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

    private String userId;      // FK vers User._id

    @Indexed(unique = true)
    private String jwtToken;

    private String ipAddress;
    private String userAgent;

    private LocalDateTime issuedAt;

    /**
     * TTL index : MongoDB supprimera ce document automatiquement après expiration.
     * @Indexed(expireAfterSeconds = 0) + valeur datetime → TTL dynamique.
     * SANS Spring : DELETE FROM sessions WHERE expires_at < NOW() via un @Scheduled job.
     */
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean isRevoked = false;

    /** Révoque le token (blacklist) — utilisé lors du logout. */
    public void revoke() {
        this.isRevoked = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}