
// ═══════════════════════════════════════════════════════
// LevelConfigRepository.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.repository;

import ma.emsi.game_platform_backend.gamification.model.LevelConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LevelConfigRepository extends MongoRepository<LevelConfig, String> {

    /** Trouve le niveau correspondant au total de points du joueur. */
    Optional<LevelConfig> findByMinPointsLessThanEqualAndMaxPointsGreaterThanEqual(
            int points1, int points2
    );

    Optional<LevelConfig> findByLevel(int level);

    /** Prochain niveau au-dessus du niveau actuel. */
    Optional<LevelConfig> findFirstByLevelGreaterThanOrderByLevelAsc(int currentLevel);
}
