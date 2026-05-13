package ma.emsi.game_platform_backend.iam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document MongoDB pour la réinitialisation de mot de passe.
 *
 * RG-04 : expiresAt = now + 1h
 * TTL index MongoDB → suppression automatique après expiration.
 *
 * SANS Spring :
 *   - Génération du token : UUID.randomUUID() manuellement
 *   - Stockage dans une table SQL avec requête JDBC INSERT
 *   - Vérification : SELECT * FROM reset_tokens WHERE token=? AND used=false AND expires_at > NOW()
 *   - Nettoyage : DELETE FROM reset_tokens WHERE expires_at < NOW() (via cron job)
 *
 * AVEC Spring Data MongoDB :
 *   - @Document + @Indexed(expireAfterSeconds=0) → TTL automatique côté MongoDB
 *   - PasswordResetTokenRepository.findByTokenAndUsedFalse() → méthode générée automatiquement
 */
@Document(collection = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    private String id;

    private String userId;  // FK vers User._id

    @Indexed(unique = true)
    @Builder.Default
    private String token = UUID.randomUUID().toString();

    /** RG-04 : TTL de 1 heure. MongoDB supprime automatiquement ce document. */
    @Indexed(expireAfterSeconds = 0)
    @Builder.Default
    private LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

    @Builder.Default
    private boolean used = false;

    private LocalDateTime createdAt;

    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }

    public void invalidate() {
        this.used = true;
    }
}
