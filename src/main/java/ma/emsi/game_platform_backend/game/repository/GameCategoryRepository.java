package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.GameCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GameCategoryRepository extends MongoRepository<GameCategory, String> {

    List<GameCategory> findByGameId(String gameId);

    void deleteByGameId(String gameId);
    void deleteByCategoryId(String categoryId);
}