// ═══════════════════════════════════════════════════════
// BadgeRepository.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.repository;

import ma.emsi.game_platform_backend.gamification.model.Badge;
import ma.emsi.game_platform_backend.shared.enums.BadgeType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadgeRepository extends MongoRepository<Badge, String> {
    List<Badge> findByIsActiveTrue();
    List<Badge> findByTypeAndIsActiveTrue(BadgeType type);
    boolean     existsByName(String name);
}
