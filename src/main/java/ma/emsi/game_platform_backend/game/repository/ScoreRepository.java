package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.Score;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends MongoRepository<Score, String> {

    List<Score> findTop10ByGameIdOrderByValueDesc(String gameId);

    Optional<Score> findTopByUserIdAndGameIdOrderByValueDesc(String userId, String gameId);

    List<Score> findByGameIdOrderByValueDesc(String gameId);
}