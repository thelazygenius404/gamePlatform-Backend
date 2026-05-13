package ma.emsi.game_platform_backend.iam.repository;


import ma.emsi.game_platform_backend.iam.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les tokens de réinitialisation de mot de passe.
 *
 * Spring Data génère automatiquement :
 * findByTokenAndUsedFalse → { "token": value, "used": false }
 *
 * SANS Spring : SELECT * FROM reset_tokens WHERE token=? AND used=false (JDBC)
 * ou db.password_reset_tokens.findOne({ token: t, used: false }) (MongoDB natif)
 */
@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    void deleteByUserId(String userId);
}