
// ═══════════════════════════════════════════════════════
// UserBadgeRepository.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.repository;

import ma.emsi.game_platform_backend.gamification.model.UserBadge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends MongoRepository<UserBadge, String> {

    /** RG-15 : vérifie si le badge est déjà attribué à cet user. */
    boolean existsByUserIdAndBadgeId(String userId, String badgeId);

    List<UserBadge> findByUserIdOrderByAwardedAtDesc(String userId);

    /** Nombre total de parties utilisées pour les badges PLAYS_COUNT. */
    long countByUserId(String userId);
}
